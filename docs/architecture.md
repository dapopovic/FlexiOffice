# FlexiOffice Architecture Overview

This document provides a concise overview of the FlexiOffice app architecture and key flows. Diagrams are in Mermaid format for readability in GitHub.

## System Context

```mermaid
%%{init: { 'theme': 'default' }}%%
flowchart TB
  user["User<br>(Employee / Manager)"]
  subgraph device[Android Device]
    app["FlexiOffice App<br>(Jetpack Compose)"]
    subgraph bg[Background Components]
      fcmSvc["FlexiOfficeMessagingService<br>(FCM)"]
      geoSvc["GeofencingService<br>(Foreground)"]
      brGeofence["GeofencingBroadcastReceiver"]
      brBoot["BootReceiver"]
    end
  end
  gps["Google Play Services<br>(Location / Geofencing)"]
  fauth["Firebase Auth"]
  fs["Cloud Firestore"]
  fcm["Firebase Cloud Messaging"]
  subgraph backend[Notification Backend]
    node["Node.js/Express Server<br>server/server.js"]
  end

  user <--> app
  app --> fauth
  app <--> fs
  app <--> fcm
  app <--> gps
  node <--> fs
  node --> fcm
  fcm --> fcmSvc
  brGeofence --> geoSvc
  brBoot --> geoSvc
```

## Containers & Components

```mermaid
flowchart LR
  subgraph App[Android App]
    direction TB
    UI["UI (Jetpack Compose)<br>- Screens: Login, Calendar, Booking, Teams, Requests, Profile<br>- Navigation: FlexiOfficeNavigation"]
    VM["ViewModels<br>Auth, Main, Calendar, Booking, Team, Requests, InAppNotification, GeofencingSettings"]
    DI["DI: Hilt Modules<br>FirebaseModule"]
    DATA["Data Layer<br>Repositories: Auth, User, Team, Booking, Notification<br>Models: User, Team, Booking, TeamInvitation, CalendarEvent"]
    BG["Background<br>FCM Service, Geofencing Service, BroadcastReceivers"]
  end

  subgraph Firebase[Firebase]
    Auth[Auth]
    Firestore[Firestore]
    FCM[Messaging]
  end

  subgraph Google[Google Play Services]
    Location[Location & Geofencing]
  end

  subgraph Backend[Notification Backend]
    Node[Node/Express Server]
  end

  UI --> VM
  VM --> DATA
  DI --> VM
  DI --> DATA
  DATA <--> Firestore
  DATA --> Auth
  BG --> DATA
  BG --> VM
  BG --> UI
  BG <--> Location
  Node <--> Firestore
  Node --> FCM
  FCM --> BG
```

## Key Sequences

### 1) Team invitation acceptance

```mermaid
sequenceDiagram
  autonumber
  participant Manager as Manager (App)
  participant FS as Firestore
  participant App as Invitee (App)
  participant Repo as TeamRepository
  participant NotiRepo as NotificationRepository
  participant Node as Node/Express
  participant FCM as Firebase Cloud Messaging
  participant Svc as FlexiOfficeMessagingService

  Manager->>Repo: createTeamInvitation(teamId, inviteeEmail)
  Repo->>FS: Transaction: create TeamInvitation (PENDING)
  Repo-->>Manager: Invitation created
  Repo->>NotiRepo: sendTeamInvitationNotification(invitation)
  NotiRepo->>FS: Write notifications doc {type: team_invitation, processed: false}
  Note right of Node: Listener on FS 'notifications' where processed==false
  Node->>FS: onSnapshot: new notification
  Node->>FCM: send message (data+notification)
  FCM->>Svc: deliver push (background)
  Svc->>App: show notification / in-app banner

  App->>Repo: acceptTeamInvitation(invitationId)
  Repo->>FS: Transaction: set invitation.status=ACCEPTED, add user to team, set user.role=USER
  Repo-->>App: Invitation updated
  Repo->>NotiRepo: sendTeamInvitationResponseNotification(invitation)
  NotiRepo->>FS: Write notifications doc {type: team_invitation_response}
  Node->>FCM: send message to manager
  FCM->>Svc: deliver push
  Svc->>App: show notification
```

### 2) Geofence exit -> home-office reminder

```mermaid
sequenceDiagram
  autonumber
  participant GPS as Google Play Services (Geofencing)
  participant BR as GeofencingBroadcastReceiver
  participant Svc as GeofencingService (Foreground)
  participant Auth as AuthRepository
  participant User as UserRepository
  participant Book as BookingRepository
  participant Noti as HomeOfficeNotificationManager

  GPS-->>BR: EXIT event (home_geofence)
  BR->>Svc: startForegroundService()
  activate Svc
  Svc->>Svc: startForeground(notification)
  Svc->>Svc: isNetworkAvailable()
  alt Network OK
    Svc->>Auth: currentUser.first()
    Auth-->>Svc: FirebaseUser
    Svc->>User: getUserStream(uid).first()
    User-->>Svc: Result<User>
    Svc->>Book: getUserBookingsForDate(uid, today)
    Book-->>Svc: Result<List<Booking>>
    alt Has APPROVED booking today
      Svc->>Noti: showHomeOfficeReminderNotification(user.name)
      Noti-->>Svc: notify()
    else No approved booking
      Svc->>Svc: no-op
    end
  else Network issues
    Svc->>Svc: retry with exponential backoff (max 3)
  end
  deactivate Svc
```

### 3) User login and FCM token registration

```mermaid
%% Refer to docs/diagrams/sequence-auth-login.mmd for standalone file
sequenceDiagram
  autonumber
  participant UI as Login Screen (Compose)
  participant VM as AuthViewModel
  participant Auth as AuthRepository
  participant FA as FirebaseAuth
  participant User as UserRepository
  participant FCM as FCMTokenManager
  participant FS as Firestore
  participant Svc as FlexiOfficeMessagingService

  UI->>VM: signIn(email, password)
  VM->>Auth: signInWithEmailAndPassword(email, password)
  Auth->>FA: signInWithEmailAndPassword()
  FA-->>Auth: FirebaseUser
  Auth-->>VM: Result<FirebaseUser>
  alt First login or no profile
    VM->>User: getUser(uid)
    User-->>VM: Result<User?> (null)
    VM->>User: createUser(uid, createDefaultUser(email))
    User-->>VM: Result<Unit>
  else Existing profile
    VM->>User: getUser(uid)
    User-->>VM: Result<User?>
  end

  Note over UI,FCM: App process creates FlexiOfficeApplication which initializes FCM
  VM-->>FCM: (via App) initializeFCM()
  FCM->>FCM: retrieve token
  FCM->>FS: set users/{uid}.fcmToken
  FS-->>FCM: OK

  Note over Svc: If token rotates later
  Svc->>Svc: onNewToken(token)
  Svc->>FCM: updateToken(token)
  FCM->>FS: set users/{uid}.fcmToken (merge)
```

### 4) Create booking and notify manager

```mermaid
%% Refer to docs/diagrams/sequence-booking-create.mmd for standalone file
sequenceDiagram
  autonumber
  participant UI as Booking Screen
  participant VM as BookingViewModel
  participant Book as BookingRepository
  participant Team as Team (Firestore)
  participant FS as Firestore (Bookings)
  participant Noti as NotificationRepository
  participant Node as Node/Express
  participant FCM as Firebase Cloud Messaging
  participant Svc as Manager App (MessagingService)

  UI->>VM: createBooking(date, comment)
  VM->>Book: createBooking(date, comment, userId, userName, teamId)
  Book->>Team: get team to ensure managerId
  Team-->>Book: Team(managerId)
  Book->>FS: add bookings/{id} (PENDING, reviewerId=managerId)
  FS-->>Book: doc id
  Book-->>VM: Result<bookingId>

  VM->>Noti: sendNewBookingRequestNotification(booking)
  Noti->>FS: add notifications { type: new_booking_request, processed: false }
  Note right of Node: Listener on notifications where processed==false
  Node->>FCM: send message to manager
  FCM->>Svc: deliver push
  Svc->>Svc: show notification or in-app banner
```

### 5) Approve/decline booking and notify requester

```mermaid
%% Refer to docs/diagrams/sequence-booking-approve.mmd for standalone file
sequenceDiagram
  autonumber
  participant UI as Requests Screen (Manager)
  participant VM as RequestsViewModel
  participant Book as BookingRepository
  participant FS as Firestore (Bookings)
  participant Noti as NotificationRepository
  participant Node as Node/Express
  participant FCM as Firebase Cloud Messaging
  participant Svc as Requester App (MessagingService)

  UI->>VM: approveOrDecline(bookingId, status)
  VM->>Book: updateBookingStatus(bookingId, status, reviewerId)
  Book->>FS: update booking.status, booking.reviewerId
  FS-->>Book: OK

  VM->>Noti: sendBookingStatusNotification(booking, status, reviewerName)
  Noti->>FS: add notifications { type: booking_status_update, processed: false }
  Note right of Node: Listener on notifications where processed==false
  Node->>FCM: send message to requester
  FCM->>Svc: deliver push
  Svc->>Svc: show notification or in-app banner
```

## Technology choices

- UI: Jetpack Compose, Material3, Navigation Compose
- DI: Hilt (FirebaseModule provides Auth, Firestore, Messaging)
- Data: Repositories wrapping Firebase Auth/Firestore; Flows for realtime updates
- Notifications: FCM push via Firestore-triggered Node server; In-app banner or system notifications
- Background: Geofencing (Google Play Services), ForegroundService for checks, Boot receiver for re-registration
- Testing/Coverage: JUnit, MockK, Turbine, Jacoco, Ktlint

## Packages at a glance

- `presentation/` — ViewModels and Compose screens/components
- `data/` — Repositories and models for Auth, User, Team, Booking, Notification
- `fcm/` — FCM service and token management
- `geofencing/` — Manager, Service, permissions, and notifications
- `broadCastReceiver/` — Geofence and boot receivers
- `di/` — Hilt modules
- `navigation/` — Routes and navigation graph
- `util/` — Utilities (Logger, ShakeDetector)

## Notes

- Team membership is enforced via invitations; acceptance updates user.role and team.members atomically.
- The Node server polls Firestore 'notifications' (processed=false) to dispatch FCM reliably.
- Geofencing uses enter+exit to stabilize state; service uses retry with backoff and keeps a daily notification guard.
