const express = require("express");
const admin = require("firebase-admin");
const bodyParser = require("body-parser");

// Initialize Firebase Admin SDK
const serviceAccount = require("./serviceAccountKey.json");
admin.initializeApp({
    credential: admin.credential.cert(serviceAccount),
});

const db = admin.firestore();
const messaging = admin.messaging();

const app = express();
app.use(bodyParser.json());

// Health check endpoint
app.get("/health", (req, res) => {
    res.json({ status: "OK", timestamp: new Date().toISOString() });
});

/**
 * Endpoint to manually trigger notification processing
 * Useful for testing and batch processing
 */
app.post("/process-notifications", async (req, res) => {
    try {
        console.log("Starting manual notification processing...");

        // Get unprocessed notifications
        const snapshot = await db
            .collection("notifications")
            .where("processed", "==", false)
            .orderBy("createdAt", "asc")
            .limit(50) // Process in batches
            .get();

        if (snapshot.empty) {
            return res.json({
                success: true,
                message: "No unprocessed notifications found",
                processed: 0,
            });
        }

        const results = [];

        for (const doc of snapshot.docs) {
            const notificationData = doc.data();
            const notificationId = doc.id;

            try {
                const result = await processNotification(notificationId, notificationData);
                results.push({ id: notificationId, status: "success", result });
            } catch (error) {
                console.error(`Error processing notification ${notificationId}:`, error);
                results.push({ id: notificationId, status: "error", error: error.message });

                // Mark as processed with error
                await markAsProcessed(notificationId, `error: ${error.message}`);
            }
        }

        res.json({
            success: true,
            message: "Notification processing completed",
            processed: results.length,
            results,
        });
    } catch (error) {
        console.error("Error in manual notification processing:", error);
        res.status(500).json({
            success: false,
            error: error.message,
        });
    }
});

/**
 * Endpoint to send a test notification
 */
app.post("/send-test-notification", async (req, res) => {
    try {
        const { fcmToken, title, body, data } = req.body;

        if (!fcmToken) {
            return res.status(400).json({
                success: false,
                error: "FCM token is required",
            });
        }

        const message = {
            token: fcmToken,
            notification: {
                title: title || "Test Notification",
                body: body || "This is a test notification from FlexiOffice",
            },
            data: {
                type: "test",
                timestamp: Date.now().toString(),
                ...data,
            },
            android: {
                notification: {
                    channelId: "flexioffice_notifications",
                    sound: "default",
                },
            },
        };

        const response = await messaging.send(message);

        res.json({
            success: true,
            messageId: response,
            message: "Test notification sent successfully",
        });
    } catch (error) {
        console.error("Error sending test notification:", error);
        res.status(500).json({
            success: false,
            error: error.message,
        });
    }
});

/**
 * Process a single notification
 */
async function processNotification(notificationId, notificationData) {
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
            timestamp: notificationData.createdAt.toString(),
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

app.listen(PORT, () => {
    console.log(`FlexiOffice notification server running on port ${PORT}`);
    console.log("Setting up real-time notification processing...");

    // Set up the Firestore listener for real-time processing
    const unsubscribe = setupFirestoreListener();

    // Graceful shutdown
    process.on("SIGTERM", () => {
        console.log("Received SIGTERM signal, shutting down gracefully...");
        unsubscribe();
        process.exit(0);
    });

    process.on("SIGINT", () => {
        console.log("Received SIGINT signal, shutting down gracefully...");
        unsubscribe();
        process.exit(0);
    });
});

module.exports = app;
