package com.doodlecat.wallpaper.anim

/** High-level lifecycle of the cat. It sleeps almost always. */
enum class CatState { SLEEPING, REACT, MOVING, SETTLING }

/** How the cat travels. All styles are short and subtle. */
enum class MoveStyle { SLIDE, HOP, SCAMPER }

/** Eyes rendering mode. */
enum class EyeMode { CLOSED, OPEN, LEFT_OPEN, RIGHT_OPEN }

/** Rare, peaceful micro-events while sleeping. */
enum class RareEvent { EAR_TWITCH, TAIL_TWITCH, ONE_EYE, MICRO_SHIFT, POSE_CHANGE, ZZZ }

/** A floating "z" sprite. Pooled, never allocated per frame. */
class ZSprite {
    var spawnMs: Long = 0L
    var dxUnits: Float = 0f
    var jitter: Float = 1f
    var alive: Boolean = false
}
