import { LitElement, html, css } from 'lit';
import { customElement, property } from 'lit/decorators.js';
import { ChatMessage, CHARACTER_COLORS, CHARACTER_SHORT_NAMES } from './types.js';

@customElement('room-chat-panel')
export class RoomChatPanel extends LitElement {
  @property({ type: String }) roomId = '';
  @property({ type: String }) roomName = '';
  @property({ type: Array }) messages: ChatMessage[] = [];

  static styles = css`
    :host {
      display: flex;
      flex-direction: column;
      width: calc(33.333% - 1px);
      flex: 0 0 auto;
      border-right: 1px solid #333;
      border-bottom: 1px solid #333;
      overflow: hidden;
    }
    :host(:nth-child(3n)) { border-right: none; }
    .header {
      padding: 8px 12px;
      background: #222;
      font-size: 12px;
      font-weight: 600;
      color: #aaa;
      text-transform: uppercase;
      letter-spacing: 1px;
      border-bottom: 1px solid #333;
    }
    .messages {
      flex: 1;
      overflow-y: auto;
      padding: 8px;
      display: flex;
      flex-direction: column;
      gap: 6px;
    }
    .message {
      padding: 6px 10px;
      border-radius: 8px;
      background: #252535;
      font-size: 13px;
      line-height: 1.4;
      border-left: 3px solid var(--char-color, #666);
    }
    .sender {
      font-weight: 600;
      font-size: 11px;
      margin-bottom: 2px;
      color: var(--char-color, #aaa);
    }
    .content { color: #ddd; white-space: pre-wrap; }
    .empty { color: #555; font-style: italic; padding: 20px; text-align: center; font-size: 12px; }
  `;

  render() {
    const roomMessages = this.messages.filter(m => m.room === this.roomId && !m.isAside);
    return html`
      <div class="header">${this.roomName}</div>
      <div class="messages">
        ${roomMessages.length === 0
          ? html`<div class="empty">The room is quiet...</div>`
          : roomMessages.map(m => this.renderMessage(m))}
      </div>
    `;
  }

  private renderMessage(m: ChatMessage) {
    const color = CHARACTER_COLORS[m.characterId] || '#666';
    const name = CHARACTER_SHORT_NAMES[m.characterId] || m.characterId;
    return html`
      <div class="message" style="--char-color: ${color}">
        <div class="sender">${name}</div>
        <div class="content">${m.content}</div>
      </div>
    `;
  }

  updated() {
    const container = this.shadowRoot?.querySelector('.messages');
    if (container) container.scrollTop = container.scrollHeight;
  }
}
