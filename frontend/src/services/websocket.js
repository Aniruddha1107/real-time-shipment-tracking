import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

class WebSocketService {
    constructor() {
        this.client = null;
        this.subscriptions = new Map();
        this.onConnectCallbacks = [];
        this.token = null;

        // Auto-reconnect on network resume
        window.addEventListener('online', () => {
            console.log('Network online. Reconnecting STOMP...');
            if (this.token && (!this.client || !this.client.connected)) {
                this.connect(this.token);
            }
        });

        window.addEventListener('offline', () => {
            console.log('Network offline. Disconnecting STOMP...');
            this.disconnect(true);
        });
    }

    connect(token) {
        if (this.client && this.client.connected) return;
        this.token = token || null;

        const socket = new SockJS('http://localhost:8084/ws');
        const connectHeaders = token ? { 'Authorization': `Bearer ${token}` } : {};
        this.client = new Client({
            webSocketFactory: () => socket,
            connectHeaders,
            debug: (str) => {
                // console.log('STOMP: ' + str); // Disabled for cleaner logs
            },
            reconnectDelay: 5000,
            heartbeatIncoming: 4000,
            heartbeatOutgoing: 4000,
        });

        this.client.onConnect = (frame) => {
            console.log('✅ Connected to STOMP broker');
            this.onConnectCallbacks.forEach(callback => callback(frame));
        };

        this.client.onWebSocketClose = () => {
            console.warn('⚠️ WebSocket Closed. Attempting reconnect...');
        };

        this.client.onStompError = (frame) => {
            console.error('Broker reported error: ' + frame.headers['message']);
            console.error('Additional details: ' + frame.body);
        };

        this.client.activate();
    }

    subscribe(topic, callback) {
        if (!this.client) {
            console.warn('Cannot subscribe, client not initialized');
            return;
        }

        const subscribeFn = () => {
            // Prevent duplicate subscriptions to the same topic
            if (this.subscriptions.has(topic)) {
                this.unsubscribe(topic);
            }
            
            const subscription = this.client.subscribe(topic, (message) => {
                callback(JSON.parse(message.body));
            });
            this.subscriptions.set(topic, subscription);
            console.log(`📡 Subscribed to ${topic}`);
        };

        if (this.client.connected) {
            subscribeFn();
        } else {
            this.onConnectCallbacks.push(subscribeFn);
        }
    }

    unsubscribe(topic) {
        const subscription = this.subscriptions.get(topic);
        if (subscription) {
            subscription.unsubscribe();
            this.subscriptions.delete(topic);
            console.log(`🔌 Unsubscribed from ${topic}`);
        }
    }

    send(destination, body) {
        if (this.client && this.client.connected) {
            this.client.publish({
                destination,
                body: JSON.stringify(body)
            });
        } else {
            console.error('STOMP client not connected');
        }
    }

    disconnect(isOfflineEvent = false) {
        if (this.client) {
            this.client.deactivate();
            if (!isOfflineEvent) {
                this.subscriptions.clear();
                this.onConnectCallbacks = [];
                this.token = null;
            }
            console.log('STOMP client deactivated');
        }
    }
}

const instance = new WebSocketService();
export default instance;
