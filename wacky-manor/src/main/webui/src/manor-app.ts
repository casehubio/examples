import { LitElement, html, css } from 'lit';
import { customElement, state } from 'lit/decorators.js';
import { ManorEvent, CharacterSnapshot, RoomSnapshot, ChatMessage } from './types.js';
import './manor-view.js';
import './room-chat-panel.js';
import './narrator-panel.js';
import './character-profile.js';

@customElement('manor-app')
export class ManorApp extends LitElement {
  @state() private characters: CharacterSnapshot[] = [];
  @state() private rooms: RoomSnapshot[] = [];
  @state() private messages: ChatMessage[] = [];
  @state() private narratorEntries: Array<{ type: 'narration' | 'aside'; characterId?: string; content: string; timestamp: number }> = [];
  @state() private scenarioStatus: 'idle' | 'running' | 'completed' = 'idle';
  @state() private activeScene: string | null = null;
  @state() private connected = false;
  @state() private paused = false;
  @state() private speed = 1.0;
  @state() private selectedCharacterId: string | null = null;

  private ws: WebSocket | null = null;

  static styles = css`
    :host {
      display: flex;
      flex-direction: column;
      height: 100vh;
      background: #1a1a2e;
    }
    .toolbar {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 8px 16px;
      background: #16162a;
      border-bottom: 1px solid #333;
    }
    .title {
      font-family: Georgia, serif;
      font-size: 18px;
      font-weight: 700;
      color: #daa520;
      letter-spacing: 2px;
    }
    .controls { display: flex; align-items: center; gap: 12px; }
    .status {
      font-size: 11px;
      padding: 4px 8px;
      border-radius: 4px;
      text-transform: uppercase;
      letter-spacing: 1px;
    }
    .status.idle { background: #333; color: #888; }
    .status.running { background: #1a3a1a; color: #4a4; }
    .status.completed { background: #3a3a1a; color: #aa4; }
    .dot { width: 6px; height: 6px; border-radius: 50%; display: inline-block; margin-right: 4px; }
    .dot.on { background: #4a4; }
    .dot.off { background: #a44; }
    button {
      padding: 6px 16px;
      border: 1px solid #555;
      background: #2a2a4a;
      color: #ddd;
      border-radius: 4px;
      cursor: pointer;
      font-size: 13px;
      font-weight: 600;
    }
    button:hover { background: #3a3a5a; }
    button:disabled { opacity: 0.4; cursor: not-allowed; }
    .manor-section {
      padding: 4px 8px;
      border-bottom: 1px solid #333;
      background: #16162a;
      position: relative;
    }
    .panels {
      display: flex;
      flex: 1;
      min-height: 0;
      overflow: hidden;
    }
    .chat-columns {
      display: grid;
      grid-template-columns: 1fr 1fr 1fr;
      grid-template-rows: 1fr 1fr;
      flex: 1;
      min-height: 0;
      overflow: hidden;
    }
    .transport { display: flex; align-items: center; gap: 4px; }
    .transport button { padding: 4px 10px; font-size: 12px; }
    .transport button.active { background: #4a4a6a; border-color: #88a; }
    .speed-group { display: flex; gap: 2px; }
    .scene-banner {
      padding: 6px 16px;
      background: #3a2a1a;
      color: #daa520;
      font-family: Georgia, serif;
      font-style: italic;
      font-size: 13px;
      text-align: center;
      border-bottom: 1px solid #554422;
    }
  `;

  connectedCallback() {
    super.connectedCallback();
    this.connectWebSocket();
    this.addEventListener('character-selected', ((e: CustomEvent) => {
      this.selectedCharacterId =
        this.selectedCharacterId === e.detail.characterId ? null : e.detail.characterId;
    }) as EventListener);
    this.addEventListener('profile-close', () => {
      this.selectedCharacterId = null;
    });
  }

  disconnectedCallback() {
    super.disconnectedCallback();
    this.ws?.close();
  }

  private connectWebSocket() {
    const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:';
    this.ws = new WebSocket(`${protocol}//${location.host}/ws/manor`);

    this.ws.onopen = () => { this.connected = true; };
    this.ws.onclose = () => {
      this.connected = false;
      setTimeout(() => this.connectWebSocket(), 3000);
    };
    this.ws.onmessage = (e) => {
      const event: ManorEvent = JSON.parse(e.data);
      this.handleEvent(event);
    };
  }

  private handleEvent(event: ManorEvent) {
    switch (event.type) {
      case 'snapshot':
        this.characters = [...event.characters];
        this.rooms = [...event.rooms];
        break;
      case 'position':
        this.characters = this.characters.map(c =>
          c.id === event.characterId ? { ...c, room: event.room, x: event.x } : c);
        break;
      case 'dialogue':
        this.messages = [...this.messages, {
          characterId: event.characterId, room: event.room,
          content: event.content, timestamp: Date.now(),
        }];
        break;
      case 'aside':
        this.narratorEntries = [...this.narratorEntries, {
          type: 'aside', characterId: event.characterId,
          content: event.content, timestamp: Date.now(),
        }];
        break;
      case 'narrator':
        this.narratorEntries = [...this.narratorEntries, {
          type: 'narration', content: event.content, timestamp: Date.now(),
        }];
        break;
      case 'scene':
        this.activeScene = event.status === 'started' ? event.sceneId : null;
        break;
      case 'scenario':
        this.scenarioStatus = event.status === 'started' ? 'running' : 'completed';
        break;
      case 'control':
        if (event.status === 'paused' || event.status === 'resumed') {
          this.paused = event.status === 'paused';
        }
        this.speed = event.speedMultiplier;
        break;
    }
  }

  private async togglePause() {
    await fetch(this.paused ? '/manor/resume' : '/manor/pause', { method: 'POST' });
  }

  private async setSpeed(rate: number) {
    await fetch(`/manor/speed?rate=${rate}`, { method: 'POST' });
  }

  private async startScenario() {
    this.messages = [];
    this.narratorEntries = [];
    const resp = await fetch('/manor/start', { method: 'POST' });
    if (resp.ok) {
      this.scenarioStatus = 'running';
    }
  }

  private async stopScenario() {
    const resp = await fetch('/manor/stop', { method: 'POST' });
    if (resp.ok) {
      this.scenarioStatus = 'completed';
    }
  }

  render() {
    return html`
      <div class="toolbar">
        <span class="title">WACKY MANOR</span>
        <div class="controls">
          <span class="dot ${this.connected ? 'on' : 'off'}"></span>
          <span class="status ${this.scenarioStatus}">${this.scenarioStatus}</span>
          ${this.scenarioStatus === 'running' ? html`
            <div class="transport">
              <button @click=${() => this.togglePause()}>
                ${this.paused ? '▶ Play' : '⏸ Pause'}
              </button>
              <div class="speed-group">
                ${[0.5, 1, 2, 4].map(s => html`
                  <button class=${this.speed === s ? 'active' : ''}
                          @click=${() => this.setSpeed(s)}>${s}x</button>
                `)}
              </div>
            </div>
            <button @click=${this.stopScenario}>⏹ Stop</button>
          ` : ''}
          <button @click=${this.startScenario}
                  ?disabled=${this.scenarioStatus === 'running'}>
            ${this.scenarioStatus === 'completed' ? '▶ Restart' : '▶ Start'}
          </button>
        </div>
      </div>

      <div class="manor-section">
        <manor-view
          .characters=${this.characters}
          .rooms=${this.rooms}
          .activeRoom=${this.activeScene ? null : null}>
        </manor-view>
        ${this.selectedCharacterId ? html`
          <character-profile .characterId=${this.selectedCharacterId}></character-profile>
        ` : ''}
      </div>

      ${this.activeScene ? html`
        <div class="scene-banner">
          Scene in progress: ${this.activeScene.replace(/-/g, ' ')}
        </div>
      ` : ''}

      <div class="panels">
        <div class="chat-columns">
          <room-chat-panel roomId="entrance-hall" roomName="Entrance Hall" .messages=${this.messages}></room-chat-panel>
          <room-chat-panel roomId="kitchen" roomName="Kitchen" .messages=${this.messages}></room-chat-panel>
          <room-chat-panel roomId="ballroom" roomName="Ballroom" .messages=${this.messages}></room-chat-panel>
          <room-chat-panel roomId="library" roomName="Library" .messages=${this.messages}></room-chat-panel>
          <room-chat-panel roomId="laboratory" roomName="Laboratory" .messages=${this.messages}></room-chat-panel>
          <room-chat-panel roomId="cellar" roomName="Cellar" .messages=${this.messages}></room-chat-panel>
        </div>
        <narrator-panel .entries=${this.narratorEntries}></narrator-panel>
      </div>
    `;
  }
}
