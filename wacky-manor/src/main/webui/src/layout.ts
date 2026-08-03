export interface RoomLayout { row: number; col: number; }

export interface CharacterSnapshot {
  id: string;
  name: string;
  room: string;
  x: number;
  active: boolean;
}

export interface CharPos {
  id: string;
  name: string;
  absX: number;
  baseY: number;
  labelRow: number;
}

export const ROOM_GRID: Record<string, RoomLayout> = {
  'entrance-hall': { row: 0, col: 0 },
  'kitchen':       { row: 0, col: 1 },
  'ballroom':      { row: 0, col: 2 },
  'library':       { row: 1, col: 0 },
  'laboratory':    { row: 1, col: 1 },
  'cellar':        { row: 1, col: 2 },
};

const OVERLAP_THRESHOLD = 30;

export function layoutCharacters(
  characters: CharacterSnapshot[], roomW: number, roomH: number, rowGap: number, topY: number
): CharPos[] {
  const minSpacing = OVERLAP_THRESHOLD;
  const positions: CharPos[] = [];

  const byRoom = new Map<string, { c: CharacterSnapshot; origX: number }[]>();
  for (const c of characters.filter(c => c.active)) {
    if (!ROOM_GRID[c.room]) continue;
    if (!byRoom.has(c.room)) byRoom.set(c.room, []);
    byRoom.get(c.room)!.push({ c, origX: c.x });
  }

  for (const [roomId, chars] of byRoom) {
    const layout = ROOM_GRID[roomId];
    const rx = layout.col * roomW + 4;
    const rw = roomW - 8;
    const roomY = topY + layout.row * (roomH + rowGap);
    const baseY = roomY + roomH - 35;
    const margin = 15;

    chars.sort((a, b) => a.origX - b.origX);
    const displayXs: number[] = chars.map(ch => rx + ch.origX * (rw - 2 * margin) + margin);

    for (let i = 1; i < displayXs.length; i++) {
      if (displayXs[i] - displayXs[i - 1] < minSpacing) {
        displayXs[i] = displayXs[i - 1] + minSpacing;
      }
    }

    const maxX = rx + rw - margin;
    if (displayXs.length > 0 && displayXs[displayXs.length - 1] > maxX) {
      const overflow = displayXs[displayXs.length - 1] - maxX;
      for (let i = 0; i < displayXs.length; i++) {
        displayXs[i] -= overflow * (i + 1) / displayXs.length;
      }
    }

    const roomPositions: CharPos[] = [];
    for (let i = 0; i < chars.length; i++) {
      const takenRows = new Set<number>();
      for (let j = 0; j < i; j++) {
        if (Math.abs(displayXs[i] - roomPositions[j].absX) <= minSpacing) {
          takenRows.add(roomPositions[j].labelRow);
        }
      }
      const maxDownRow = Math.floor((roomY + roomH - baseY - 14) / 10);
      let labelRow = 0;
      if (takenRows.has(0)) {
        for (let offset = 1; ; offset++) {
          if (offset <= maxDownRow && !takenRows.has(offset))  { labelRow = offset;  break; }
          if (!takenRows.has(-offset)) { labelRow = -offset; break; }
        }
      }
      const pos: CharPos = {
        id: chars[i].c.id,
        name: chars[i].c.name,
        absX: displayXs[i],
        baseY,
        labelRow,
      };
      roomPositions.push(pos);
      positions.push(pos);
    }
  }

  return positions;
}
