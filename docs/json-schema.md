# FlexiOffice

Bei uns gibt es drei Sammlungen:

**1. `users/{uid}`**:
Speichert Benutzerinformationen wie `name`, `email`, `role` (`"reviewer"` oder `"member"`) und die zugehörige `teamId`. Jeder Benutzer kann sein eigenes Profil beim ersten Login anlegen, jedoch danach nicht mehr bearbeiten.

**2. `teams/{teamId}`**:
Enthält den `name` des Teams und ein array `members[]`, das die UIDs der zugehörigen User listet. Nur Benutzer mit der Rolle `"reviewer"` dürfen Teams erstellen oder Mitglieder hinzufügen bzw. entfernen.

**3. `bookings/{bookingId}`**:
Erfasst einzelne Buchungen mit `date`, `status` (z. B. `"approved"` oder `"declined"`), `comment`, dem `ownerId` (der Ersteller der Buchung) und optional einem `reviewerId`. Nur der Owner darf eine Buchung erstellen und bearbeiten – mit Ausnahme des `status`, den ausschließlich ein `"reviewer"` ändern darf.

Das Schema ist rollenbasiert abgesichert und unterstützt klare Verantwortlichkeiten.
