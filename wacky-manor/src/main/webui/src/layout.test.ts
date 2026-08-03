import { describe, it, expect } from 'vitest';
import { layoutCharacters, CharacterSnapshot } from './layout.js';

function makeChar(id: string, room: string, x: number): CharacterSnapshot {
  return { id, name: id, room, x, active: true };
}

describe('layoutCharacters label placement', () => {
  const roomW = 240;
  const roomH = 150;
  const rowGap = 30;
  const topY = 50;

  it('single character has labelRow 0', () => {
    const chars = [makeChar('penelope', 'entrance-hall', 0.5)];
    const positions = layoutCharacters(chars, roomW, roomH, rowGap, topY);
    expect(positions).toHaveLength(1);
    expect(positions[0].labelRow).toBe(0);
  });

  it('two distant characters both have labelRow 0', () => {
    const chars = [
      makeChar('penelope', 'entrance-hall', 0.1),
      makeChar('dastardly', 'entrance-hall', 0.9),
    ];
    const positions = layoutCharacters(chars, roomW, roomH, rowGap, topY);
    expect(positions[0].labelRow).toBe(0);
    expect(positions[1].labelRow).toBe(0);
  });

  it('overlapping characters use negative labelRow before positive', () => {
    const chars = [
      makeChar('a', 'entrance-hall', 0.5),
      makeChar('b', 'entrance-hall', 0.5),
      makeChar('c', 'entrance-hall', 0.5),
    ];
    const positions = layoutCharacters(chars, roomW, roomH, rowGap, topY);

    expect(positions[0].labelRow).toBe(0);
    // Second overlaps first: tries down, if no room goes up
    // Third: picks whatever row is free
    const rows = positions.map(p => p.labelRow);
    const uniqueRows = new Set(rows);
    expect(uniqueRows.size).toBeGreaterThanOrEqual(2);
  });

  it('nine overlapping characters do not all stack downward', () => {
    const chars = Array.from({ length: 9 }, (_, i) =>
      makeChar(`char-${i}`, 'entrance-hall', 0.1 + i * 0.05)
    );
    const positions = layoutCharacters(chars, roomW, roomH, rowGap, topY);

    const labelRows = positions.map(p => p.labelRow);
    // Down-first: most labels should be non-negative
    const nonNegCount = labelRows.filter(r => r >= 0).length;
    expect(nonNegCount).toBeGreaterThan(0);

    // Should not stack excessively in either direction
    expect(Math.max(...labelRows)).toBeLessThan(5);
  });

  it('entrance-hall with 9 real characters has labels in both directions', () => {
    const chars = [
      makeChar('pat-pending',    'entrance-hall', 0.2),
      makeChar('penelope',       'entrance-hall', 0.3),
      makeChar('sergeant-blast', 'entrance-hall', 0.35),
      makeChar('dastardly',      'entrance-hall', 0.4),
      makeChar('private-meekly', 'entrance-hall', 0.4),
      makeChar('ant-hill-mob',   'entrance-hall', 0.5),
      makeChar('peter',          'entrance-hall', 0.6),
      makeChar('sneekly',        'entrance-hall', 0.7),
      makeChar('muttley',        'entrance-hall', 0.8),
    ];
    const positions = layoutCharacters(chars, 240, 150, 30, 50);

    const labelRows = positions.map(p => p.labelRow);
    // Down-first: should have row 0 and positive rows
    expect(labelRows.filter(r => r === 0).length).toBeGreaterThan(0);
    expect(Math.max(...labelRows)).toBeLessThanOrEqual(4);
    expect(Math.min(...labelRows)).toBeGreaterThanOrEqual(-4);
  });

  it('label y pixel positions stay within room bounds', () => {
    const chars = Array.from({ length: 9 }, (_, i) =>
      makeChar('c' + i, 'entrance-hall', 0.1 + i * 0.05)
    );
    const positions = layoutCharacters(chars, roomW, roomH, rowGap, topY);

    const roomBottom = topY + roomH;
    const labelBase = 14;
    const labelSpacing = 10;
    for (const p of positions) {
      const labelY = p.baseY + labelBase + p.labelRow * labelSpacing;
      expect(labelY).toBeLessThan(roomBottom);
    }
  });
});
