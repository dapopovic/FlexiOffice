const express = require("express");
const admin = require("firebase-admin");

let isShuttingDown = false;

// Initialize Firebase Admin SDK
const serviceAccount = require("./serviceAccountKey.json");
admin.initializeApp({
    credential: admin.credential.cert(serviceAccount),
});

const db = admin.firestore();
const messaging = admin.messaging();

const app = express();
app.use(express.json());

// Health check endpoint
app.get("/health", (req, res) => {
    res.json({ status: "OK", timestamp: new Date().toISOString() });
});

async function processExistingNotifications() {
    console.log("Processing existing unprocessed notifications...");

    try {
        const snapshot = await db
            .collection("notifications")
            .where("processed", "==", false)
            .orderBy("createdAt", "asc")
            .get();

        if (!snapshot.empty) {
            console.log(`Found ${snapshot.size} existing unprocessed notifications`);

            for (const doc of snapshot.docs) {
                const notificationData = doc.data();
                const notificationId = doc.id;

                try {
                    await processNotification(notificationId, notificationData);
                } catch (error) {
                    console.error(`Error processing existing notification ${notificationId}:`, error);
                    await markAsProcessed(notificationId, `error: ${error.message}`);
                }
            }
        }
    } catch (error) {
        console.error("Error processing existing notifications:", error);
    }
}

/**
 * Process a single notification
 */
async function processNotification(notificationId, notificationData) {
    if (isShuttingDown) {
        console.log("Server is shutting down, skipping notification processing");
        return;
    }
    console.log(`Processing notification ${notificationId}`, {
        type: notificationData.type,
        hasToken: !!notificationData.fcmToken,
    });

    // Validate notification data
    if (!notificationData.fcmToken) {
        throw new Error("No FCM token provided");
    }

    // Prepare FCM message
    const message = {
        token: notificationData.fcmToken,
        notification: {
            title: notificationData.title,
            body: notificationData.body,
        },
        data: {
            // Convert all data values to strings (FCM requirement)
            ...Object.fromEntries(
                Object.entries(notificationData.data || {}).map(([key, value]) => [
                    key,
                    typeof value === "string" ? value : JSON.stringify(value),
                ])
            ),
            notificationId: notificationId,
            timestamp: notificationData.createdAt
                ? notificationData.createdAt.toMillis
                    ? notificationData.createdAt.toMillis().toString()
                    : notificationData.createdAt.toString()
                : Date.now().toString(),
        },
        android: {
            notification: {
                channelId: "flexioffice_notifications",
                sound: "default",
            },
        },
    };

    // Send FCM message
    console.log(`Sending FCM message for notification ${notificationId}`);
    const response = await messaging.send(message);

    console.log(`FCM message sent successfully`, {
        notificationId,
        messageId: response,
        type: notificationData.type,
    });

    // Mark as processed
    await markAsProcessed(notificationId, "success");

    return response;
}

/**
 * Mark a notification as processed in Firestore
 */
async function markAsProcessed(notificationId, status) {
    try {
        await db.collection("notifications").doc(notificationId).update({
            processed: true,
            processedAt: Date.now(),
            processStatus: status,
        });

        console.log(`Marked notification ${notificationId} as processed: ${status}`);
    } catch (error) {
        console.error(`Failed to mark notification ${notificationId} as processed:`, error);
        throw error;
    }
}

/**
 * Set up Firestore listener for real-time notification processing
 */
function setupFirestoreListener() {
    console.log("Setting up Firestore listener for notifications...");

    const unsubscribe = db
        .collection("notifications")
        .where("processed", "==", false)
        .onSnapshot(
            async (snapshot) => {
                console.log(`Received ${snapshot.docChanges().length} notification changes`);

                for (const change of snapshot.docChanges()) {
                    if (change.type === "added") {
                        const notificationData = change.doc.data();
                        const notificationId = change.doc.id;

                        console.log(`New notification detected: ${notificationId}`);

                        try {
                            await processNotification(notificationId, notificationData);
                        } catch (error) {
                            console.error(`Error processing notification ${notificationId}:`, error);
                            await markAsProcessed(notificationId, `error: ${error.message}`);
                        }
                    }
                }
            },
            (error) => {
                console.error("Error in Firestore listener:", error);
            }
        );

    // Return the unsubscribe function for cleanup
    return unsubscribe;
}

// Start the server
const PORT = process.env.PORT || 3000;

app.listen(PORT, async () => {
    console.log(`FlexiOffice notification server running on port ${PORT}`);
    console.log("Setting up real-time notification processing...");
    // Process any existing unprocessed notifications
    await processExistingNotifications();

    // Set up the Firestore listener for real-time processing
    const unsubscribe = setupFirestoreListener();

    // Graceful shutdown
    process.on("SIGTERM", async () => {
        console.log("Received SIGTERM signal, shutting down gracefully...");
        isShuttingDown = true;
        unsubscribe();
        setTimeout(() => {
            console.log("Forcefully shutting down after timeout");
            process.exit(0);
        }, 5000); // Wait 5 seconds before forcefully exiting
    });

    process.on("SIGINT", async () => {
        console.log("Received SIGINT signal, shutting down gracefully...");
        isShuttingDown = true;
        unsubscribe();
        setTimeout(() => {
            console.log("Forcefully shutting down after timeout");
            process.exit(0);
        }, 5000); // Wait 5 seconds before forcefully exiting
    });
});

module.exports = app;
