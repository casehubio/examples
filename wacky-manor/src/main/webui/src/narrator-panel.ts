import { LitElement, html, css } from 'lit';
import { customElement, property } from 'lit/decorators.js';
import { CHARACTER_SHORT_NAMES } from './types.js';

interface NarratorEntry {
  type: 'narration' | 'aside';
  characterId?: string;
  content: string;
  timestamp: number;
}

@customElement('narrator-panel')
export class NarratorPanel extends LitElement {
  @property({ type: Array }) entries: NarratorEntry[] = [];

  static styles = css`
    :host {
      display: flex;
      flex-direction: column;
      width: 320px;
      min-width: 280px;
      background: #1e1e2a;
      border-left: 1px solid #333;
      overflow: hidden;
    }
    .header {
      padding: 8px 12px;
      background: #222;
      font-size: 12px;
      font-weight: 600;
      color: #daa520;
      text-transform: uppercase;
      letter-spacing: 1px;
      border-bottom: 1px solid #333;
    }
    .entries {
      flex: 1;
      overflow-y: auto;
      padding: 8px;
      display: flex;
      flex-direction: column;
      gap: 8px;
    }
    .narration {
      padding: 8px 12px;
      font-family: Georgia, 'Times New Roman', serif;
      font-style: italic;
      font-size: 13px;
      line-height: 1.5;
      color: #daa520;
      background: #252520;
      border-radius: 6px;
      border-left: 3px solid #daa520;
    }
    .aside {
      padding: 6px 10px;
      font-family: 'Courier New', monospace;
      font-size: 12px;
      line-height: 1.4;
      color: #cc4444;
      background: #1a1015;
      border-radius: 6px;
      border-left: 3px solid #882222;
    }
    .aside-sender {
      font-weight: 600;
      font-size: 10px;
      color: #aa3333;
      margin-bottom: 3px;
      text-transform: uppercase;
    }
    .empty { color: #555; font-style: italic; padding: 20px; text-align: center; font-size: 12px; }
  `;

  render() {
    return html`
      <div class="header">Narrator</div>
      <div class="entries">
        ${this.entries.length === 0
          ? html`<div class="empty">Waiting for the story to begin...</div>`
          : this.entries.map(e => this.renderEntry(e))}
      </div>
    `;
  }

  private renderEntry(e: NarratorEntry) {
    if (e.type === 'narration') {
      return html`<div class="narration">${e.content}</div>`;
    }
    const name = CHARACTER_SHORT_NAMES[e.characterId || ''] || e.characterId || 'Unknown';
    return html`
      <div class="aside">
        <div class="aside-sender">${name} [aside]</div>
        ${e.content}
      </div>
    `;
  }

  updated() {
    const container = this.shadowRoot?.querySelector('.entries');
    if (container) container.scrollTop = container.scrollHeight;
  }
}
