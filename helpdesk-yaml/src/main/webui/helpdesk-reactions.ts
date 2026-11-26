import {
  ApplicationScoped,
  Observes,
  EventContainer,
} from "@casehubio/pages-event-decorators";

interface TicketEvent {
  type: "CREATED" | "CLASSIFIED" | "ASSIGNED" | "RESOLVED";
  subject: string;
  category: string;
  priority: string;
  status: string;
}

interface NotificationEvent {
  to: string;
  message: string;
}

@ApplicationScoped
class HelpdeskReactions {
  @Observes("helpdesk:tickets")
  onTicketClassified(event: TicketEvent) {
    if (event.type === "CLASSIFIED") {
      console.log(`Ticket classified: ${event.category} / ${event.priority}`);
    }
  }

  @Observes("helpdesk:tickets")
  onTicketResolved(event: TicketEvent) {
    if (event.type === "RESOLVED") {
      console.log(`Ticket resolved: ${event.subject}`);
    }
  }

  @Observes("helpdesk:notifications")
  onNotification(event: NotificationEvent) {
    console.log(`Notification sent to ${event.to}: ${event.message}`);
  }
}

const wsProtocol = location.protocol === "https:" ? "wss:" : "ws:";
const container = new EventContainer({
  wsUrl: `${wsProtocol}//${location.host}/push`,
  publishBaseUrl: location.origin,
});
container.register(HelpdeskReactions);
container.start();

export { container, HelpdeskReactions };
