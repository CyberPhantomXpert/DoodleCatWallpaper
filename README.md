# Doodle Cat — Live Wallpaper

A minimalist Android live wallpaper: a hand-drawn white doodle cat sleeping on pure
black. Tap anywhere and it wakes for a moment, scampers to a new random spot, and
falls back asleep. Minimal animation, maximum charm.

## Install & run
1. Open the folder in Android Studio (Ladybug or newer; AGP 8.7, Kotlin 2.0).
2. Sync, then Run ▶ the `app` configuration on a device/emulator (API 26+).
3. The app opens on "Meet your wallpaper cat." → **Set as Live Wallpaper** →
   pick *Doodle Cat* in the system picker.
   (CLI: `adb shell am start -n com.doodlecat.wallpaper/.ui.MainActivity`)

If Gradle wrapper files are missing, run `gradle wrapper --gradle-version 8.9` once,
or let Android Studio generate them on first sync.

## Behaviour
- **Always sleeping.** After appearing/moving the cat settles and sleeps indefinitely
  with a ~1.3% breathing swell, plus very rare ear/tail twitches, one-eye peeks,
  micro-shifts, pose changes and floating "z" glyphs.
- **Tap** → 150 ms react (eyes open, tiny squash) → 220–900 ms move (slide / hop /
  scamper, randomised) → 240 ms settle bounce → sleep. Taps while moving retarget
  the in-flight move; nothing queues, nothing stacks, nothing crashes.
- **Positions** are sampled with rejection sampling against the real scaled bounding
  box of the destination pose and the real surface size, with an edge margin
  (7% of the short side, clamped 24–96 px), a minimum travel distance (18% of the
  short side) and a 5-entry recency penalty. Portrait, landscape, tablets and tiny
  screens all clamp safely; an oversized cat degenerates to screen centre.
- **Poses:** curled, loaf (paws forward), side-lying, face-down splat — same line
  weight, ears and face grammar, so one artist's hand. The reference artwork's
  stretch pose is used for wake/react moments; a low walk pose is used while moving.
- **Easter egg:** tap the cat itself 4+ times within 6 s and it gets mildly annoyed
  (a tiny "!" appears and it flees ~1.8× farther, a bit faster). It forgives quickly.

## Battery design
- No perpetual game loop. `Choreographer` frames run **only** during animation
  bursts (always < 1.5 s).
- Sleeping: an 80 ms idle tick drives breathing and "z" sprites (~11 Hz of trivial
  canvas work). If breathing and sprites cannot change anything, the controller
  returns delay `-1` and the engine schedules **zero** work until the next rare
  event or touch — the wallpaper is then literally static.
- `ACTION_SCREEN_OFF` and `onVisibilityChanged(false)` remove all callbacks;
  `ACTION_SCREEN_ON` / visibility resume restarts cleanly.
- Zero allocations in the draw path: poses are immutable `Path`s built once, paints
  and rects are reused, transforms use canvas save/restore. Vector strokes stay
  crisp at any DPI (HD → 2K+).
- No permissions at all. No network. No services besides the wallpaper service.

## Architecture
| Class | Responsibility |
|---|---|
| `CatWallpaperService` / `CatEngine` | Wallpaper lifecycle, pacing (frames vs idle tick vs nothing), touch dispatch, screen on/off |
| `CatAnimationController` | State machine (SLEEPING/REACT/MOVING/SETTLING), timing, rare events, annoyance, geometry clamping |
| `CatRenderer` | Pure canvas drawing of poses, breathing/ear/tail transforms, eyes, "z", "!" |
| `CatPoses` / `CatPose` / `Part` | Hand-drawn vector artwork in a 100-unit design space |
| `RandomPositionGenerator` | Constrained random destinations with recency penalty |
| `TouchController` | Motion events → deliberate taps only |
| `Prefs` | Primitive SharedPreferences wrapper (size, speed, rare events, Z, pose, background) |
| `MainActivity` / `SettingsActivity` / `CatPreviewView` | First-launch screen and minimalist settings |

## Settings
Cat size (S/M/L) · movement speed (slow/normal/fast) · rare events on/off ·
sleeping Z on/off · random sleeping pose on/off · charcoal background toggle
(default pure `#000000`). Reachable from the app and from the system wallpaper
picker's settings gear.
