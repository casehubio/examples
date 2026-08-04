import { LitElement, html, css, nothing } from 'lit';
import { customElement, property, state } from 'lit/decorators.js';
import { CharacterProfileResponse, CHARACTER_COLORS } from './types.js';

@customElement('character-profile')
export class CharacterProfile extends LitElement {
  @property({ type: String }) characterId: string | null = null;
  @state() private profile: CharacterProfileResponse | null = null;
  @state() private loading = false;

  static styles = css`
    :host {
      position: absolute;
      right: 8px;
      top: 8px;
      width: 280px;
      max-height: calc(100% - 16px);
      overflow-y: auto;
      background: #1e1e32;
      border: 1px solid #444;
      border-radius: 8px;
      padding: 16px;
      color: #ddd;
      font-size: 13px;
      z-index: 10;
      box-shadow: 0 4px 20px rgba(0,0,0,0.5);
    }
    h2 { margin: 0 0 4px; font-size: 16px; font-family: Georgia, serif; }
    .role { color: #999; font-size: 11px; text-transform: uppercase; letter-spacing: 1px; margin-bottom: 12px; }
    .section { margin-bottom: 12px; }
    .section-title { font-size: 11px; font-weight: 600; color: #888; text-transform: uppercase; letter-spacing: 1px; margin-bottom: 6px; }
    .bar-row { display: flex; align-items: center; gap: 6px; margin-bottom: 3px; }
    .bar-label { width: 20px; font-size: 10px; color: #aaa; text-transform: uppercase; text-align: right; }
    .bar-track { flex: 1; height: 8px; background: #2a2a3e; border-radius: 4px; overflow: hidden; }
    .bar-fill { height: 100%; border-radius: 4px; }
    .tag { display: inline-block; padding: 2px 6px; background: #2a2a4a; border-radius: 3px; font-size: 10px; color: #aaa; margin: 2px; }
    .goal, .constraint { padding: 4px 0; border-bottom: 1px solid #2a2a3e; font-size: 12px; }
    .goal:last-child, .constraint:last-child { border-bottom: none; }
    .priority, .severity { font-size: 10px; color: #888; margin-left: 4px; }
    .briefing { font-style: italic; color: #bbb; line-height: 1.5; font-size: 12px; }
    .loading { color: #666; font-style: italic; }
    .close { position: absolute; top: 8px; right: 12px; cursor: pointer; color: #666; font-size: 16px; }
    .close:hover { color: #aaa; }
    .cap-row { margin-bottom: 4px; font-size: 12px; }
  `;

  willUpdate(changed: Map<string, unknown>) {
    if (changed.has('characterId') && this.characterId) {
      this.fetchProfile();
    }
  }

  private async fetchProfile() {
    if (!this.characterId) return;
    this.loading = true;
    this.profile = null;
    try {
      const resp = await fetch(`/manor/characters/${this.characterId}/profile`);
      if (resp.ok) this.profile = await resp.json();
    } finally {
      this.loading = false;
    }
  }

  render() {
    if (!this.characterId) return nothing;
    if (this.loading) return html`<div class="loading">Loading profile...</div>`;
    if (!this.profile) return html`<div class="loading">Character not found</div>`;

    const p = this.profile;
    const color = CHARACTER_COLORS[p.agentId] || '#888';
    const maxWeight = Math.max(...p.dispositionProfile.map(d => d.weight), 0.01);

    return html`
      <span class="close" @click=${() => this.dispatchEvent(
        new CustomEvent('profile-close', { bubbles: true, composed: true }))}>&#10005;</span>
      <h2 style="color: ${color}">${p.name}</h2>
      <div class="role">${p.slotLabel || p.slot}${p.enneagramType ? ` · ${p.enneagramType}` : ''}</div>

      ${p.dispositionProfile.length > 0 ? html`
        <div class="section">
          <div class="section-title">Cognitive Functions</div>
          ${p.dispositionProfile.map(d => html`
            <div class="bar-row">
              <span class="bar-label">${d.term}</span>
              <div class="bar-track">
                <div class="bar-fill" style="width: ${(d.weight / maxWeight) * 100}%; background: ${color}"></div>
              </div>
            </div>
          `)}
        </div>
      ` : ''}

      ${p.capabilities.length > 0 ? html`
        <div class="section">
          <div class="section-title">Capabilities</div>
          ${p.capabilities.map(c => html`
            <div class="cap-row">${c.name} ${c.tags.map(t => html`<span class="tag">${t}</span>`)}</div>
          `)}
        </div>
      ` : ''}

      ${p.goals.length > 0 ? html`
        <div class="section">
          <div class="section-title">Goals</div>
          ${p.goals.map(g => html`
            <div class="goal">${g.description}<span class="priority">${g.priority}</span></div>
          `)}
        </div>
      ` : ''}

      ${p.constraints.length > 0 ? html`
        <div class="section">
          <div class="section-title">Constraints</div>
          ${p.constraints.map(c => html`
            <div class="constraint">${c.description}<span class="severity">${c.severity}</span></div>
          `)}
        </div>
      ` : ''}

      ${p.briefing ? html`
        <div class="section">
          <div class="section-title">Briefing</div>
          <div class="briefing">${p.briefing}</div>
        </div>
      ` : ''}
    `;
  }
}
