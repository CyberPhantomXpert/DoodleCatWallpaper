package com.doodlecat.wallpaper.render

import android.graphics.Path

enum class PartGroup { BODY, HEAD, EAR, TAIL, FACE }

/**
 * One drawable stroke with a pivot used for micro-rotations
 * (ear twitch, tail sway) and group transforms (breathing).
 */
class Part(
    val group: PartGroup,
    val pivotX: Float,
    val pivotY: Float,
    build: (Path) -> Unit
) {
    val path: Path = Path().also(build)
}

/**
 * A pose lives in a [unitsW] x [unitsH] box. [bodyPivot] is the breathing anchor,
 * [headAnchor] is where "z" sprites and the annoyed "!" appear.
 */
class CatPose(
    val name: String,
    val unitsW: Float,
    val unitsH: Float,
    val bodyPivotX: Float,
    val bodyPivotY: Float,
    val headAnchorX: Float,
    val headAnchorY: Float,
    val parts: List<Part>
)

private fun path(body: Path.() -> Unit): (Path) -> Unit = { p -> p.body() }

/**
 * Doodle pose library. Coordinates were drawn by hand against the reference
 * artwork's proportions (head ≈ 1/3 of body, thin limbs, hook-shaped tail).
 */
object CatPoses {

    /* ---------------- Sleeping: curled "cookie" ---------------- */
    val CURLED = CatPose(
        name = "curled", unitsW = 92f, unitsH = 80f,
        bodyPivotX = 54f, bodyPivotY = 52f, headAnchorX = 34f, headAnchorY = 44f,
        parts = listOf(
            Part(PartGroup.BODY, 54f, 52f, path {
                moveTo(46f, 22f)
                cubicTo(64f, 16f, 82f, 30f, 82f, 50f)
                cubicTo(82f, 70f, 68f, 84f, 50f, 84f)
                cubicTo(40f, 84f, 30f, 80f, 25f, 72f)
            }),
            Part(PartGroup.HEAD, 34f, 64f, path {
                moveTo(38f, 52f)
                cubicTo(30f, 50f, 22f, 56f, 22f, 64f)
                cubicTo(22f, 72f, 29f, 78f, 37f, 77f)
                cubicTo(44f, 76f, 48f, 70f, 47f, 63f)
                cubicTo(46f, 57f, 43f, 53f, 38f, 52f)
            }),
            Part(PartGroup.EAR, 28f, 52f, path {
                moveTo(26f, 54f); cubicTo(24f, 48f, 24f, 46f, 25f, 44f); cubicTo(28f, 46f, 30f, 48f, 31f, 51f)
            }),
            Part(PartGroup.EAR, 40f, 50f, path {
                moveTo(38f, 50f); cubicTo(39f, 45f, 40f, 43f, 42f, 42f); cubicTo(43f, 45f, 43f, 48f, 42f, 51f)
            }),
            Part(PartGroup.TAIL, 80f, 58f, path {
                moveTo(80f, 58f)
                cubicTo(88f, 66f, 84f, 80f, 70f, 86f)
                cubicTo(58f, 91f, 44f, 90f, 38f, 86f)
            }),
            Part(PartGroup.FACE, 34f, 64f, path {   // closed eyes
                moveTo(28f, 63f); cubicTo(29.5f, 65f, 31.5f, 65f, 33f, 63f)
                moveTo(38f, 62f); cubicTo(39.5f, 64f, 41.5f, 64f, 43f, 62f)
            }),
            Part(PartGroup.FACE, 34f, 64f, path {   // open eyes (dots)
                moveTo(30f, 63f); lineTo(30.05f, 63f)
                moveTo(40f, 62f); lineTo(40.05f, 62f)
            }),
            Part(PartGroup.FACE, 34f, 64f, path {   // mouth
                moveTo(34f, 68.5f); cubicTo(35f, 69.5f, 36f, 69.5f, 37f, 68.5f)
            })
        )
    )

    /* ---------------- Sleeping: loaf, paws forward ---------------- */
    val LOAF = CatPose(
        name = "loaf", unitsW = 92f, unitsH = 78f,
        bodyPivotX = 48f, bodyPivotY = 56f, headAnchorX = 72f, headAnchorY = 22f,
        parts = listOf(
            Part(PartGroup.BODY, 48f, 56f, path {
                moveTo(16f, 62f)
                cubicTo(14f, 50f, 24f, 42f, 40f, 41f)
                cubicTo(58f, 40f, 72f, 44f, 78f, 52f)
                cubicTo(82f, 58f, 82f, 64f, 78f, 68f)
                cubicTo(70f, 74f, 30f, 74f, 22f, 70f)
                cubicTo(18f, 68f, 16f, 66f, 16f, 62f)
            }),
            Part(PartGroup.HEAD, 72f, 37f, path {
                moveTo(62f, 40f)
                cubicTo(58f, 34f, 62f, 27f, 70f, 26f)
                cubicTo(78f, 25f, 84f, 30f, 84f, 37f)
                cubicTo(84f, 44f, 78f, 49f, 71f, 49f)
                cubicTo(66f, 49f, 63f, 45f, 62f, 40f)
            }),
            Part(PartGroup.EAR, 65f, 28f, path {
                moveTo(63f, 29f); cubicTo(62f, 24f, 62f, 22f, 63f, 20f); cubicTo(66f, 22f, 67f, 24f, 68f, 27f)
            }),
            Part(PartGroup.EAR, 79f, 26f, path {
                moveTo(77f, 26f); cubicTo(78f, 21f, 79f, 19f, 81f, 18f); cubicTo(82f, 21f, 82f, 24f, 81f, 27f)
            }),
            Part(PartGroup.TAIL, 16f, 60f, path {
                moveTo(16f, 60f)
                cubicTo(10f, 64f, 10f, 70f, 16f, 72f)
                cubicTo(24f, 75f, 34f, 75f, 40f, 74f)
            }),
            Part(PartGroup.BODY, 48f, 56f, path {   // front paws
                moveTo(78f, 66f); cubicTo(84f, 66f, 88f, 68f, 88f, 70f); cubicTo(84f, 72f, 80f, 72f, 78f, 71f)
                moveTo(68f, 72f); cubicTo(72f, 73.5f, 76f, 73.5f, 79f, 72.5f)
            }),
            Part(PartGroup.FACE, 72f, 37f, path {
                moveTo(66f, 36f); cubicTo(67.5f, 38f, 69.5f, 38f, 71f, 36f)
                moveTo(76f, 35f); cubicTo(77.5f, 37f, 79.5f, 37f, 81f, 35f)
            }),
            Part(PartGroup.FACE, 72f, 37f, path {
                moveTo(68.5f, 36.5f); lineTo(68.55f, 36.5f)
                moveTo(78.5f, 35.5f); lineTo(78.55f, 35.5f)
            }),
            Part(PartGroup.FACE, 72f, 37f, path {
                moveTo(72f, 41.5f); cubicTo(73f, 42.5f, 74f, 42.5f, 75f, 41.5f)
            })
        )
    )

    /* ---------------- Sleeping: on its side, curled tail ---------------- */
    val SIDE = CatPose(
        name = "side", unitsW = 96f, unitsH = 78f,
        bodyPivotX = 55f, bodyPivotY = 52f, headAnchorX = 27f, headAnchorY = 24f,
        parts = listOf(
            Part(PartGroup.BODY, 55f, 52f, path {
                moveTo(30f, 44f)
                cubicTo(40f, 36f, 58f, 34f, 70f, 40f)
                cubicTo(80f, 45f, 84f, 54f, 80f, 62f)
                cubicTo(76f, 70f, 62f, 74f, 50f, 72f)
                cubicTo(40f, 70f, 34f, 66f, 32f, 60f)
            }),
            Part(PartGroup.HEAD, 28f, 41f, path {
                moveTo(18f, 44f)
                cubicTo(14f, 38f, 18f, 31f, 26f, 30f)
                cubicTo(34f, 29f, 40f, 34f, 40f, 41f)
                cubicTo(40f, 48f, 34f, 53f, 27f, 53f)
                cubicTo(22f, 53f, 19f, 49f, 18f, 44f)
            }),
            Part(PartGroup.EAR, 20f, 32f, path {
                moveTo(19f, 33f); cubicTo(17f, 28f, 17f, 26f, 18f, 24f); cubicTo(21f, 26f, 22f, 28f, 23f, 31f)
            }),
            Part(PartGroup.EAR, 33f, 29f, path {
                moveTo(31f, 29f); cubicTo(32f, 24f, 33f, 22f, 35f, 21f); cubicTo(36f, 24f, 36f, 27f, 35f, 30f)
            }),
            Part(PartGroup.TAIL, 80f, 60f, path {
                moveTo(80f, 60f)
                cubicTo(88f, 58f, 92f, 64f, 88f, 70f)
                cubicTo(84f, 76f, 76f, 76f, 74f, 70f)
                cubicTo(73f, 66f, 76f, 63f, 79f, 64f)
            }),
            Part(PartGroup.BODY, 55f, 52f, path {   // tucked paw
                moveTo(46f, 72f); cubicTo(48f, 76f, 52f, 78f, 56f, 77f)
            }),
            Part(PartGroup.FACE, 28f, 41f, path {
                moveTo(22f, 40f); cubicTo(23.5f, 42f, 25.5f, 42f, 27f, 40f)
                moveTo(31f, 39f); cubicTo(32.5f, 41f, 34.5f, 41f, 36f, 39f)
            }),
            Part(PartGroup.FACE, 28f, 41f, path {
                moveTo(24.5f, 40.5f); lineTo(24.55f, 40.5f)
                moveTo(33.5f, 39.5f); lineTo(33.55f, 39.5f)
            }),
            Part(PartGroup.FACE, 28f, 41f, path {
                moveTo(27f, 45.5f); cubicTo(28f, 46.5f, 29f, 46.5f, 30f, 45.5f)
            })
        )
    )

    /* ---------------- Sleeping: face-down splat ---------------- */
    val SPRAWL = CatPose(
        name = "sprawl", unitsW = 92f, unitsH = 76f,
        bodyPivotX = 50f, bodyPivotY = 56f, headAnchorX = 50f, headAnchorY = 46f,
        parts = listOf(
            Part(PartGroup.BODY, 50f, 56f, path {
                moveTo(30f, 52f)
                cubicTo(34f, 44f, 66f, 44f, 70f, 52f)
                cubicTo(74f, 58f, 70f, 64f, 62f, 66f)
                cubicTo(52f, 68f, 44f, 68f, 38f, 66f)
                cubicTo(30f, 64f, 27f, 58f, 30f, 52f)
            }),
            Part(PartGroup.HEAD, 50f, 62f, path {
                moveTo(38f, 60f)
                cubicTo(38f, 54f, 62f, 54f, 62f, 60f)
                cubicTo(62f, 66f, 58f, 70f, 50f, 70f)
                cubicTo(42f, 70f, 38f, 66f, 38f, 60f)
            }),
            Part(PartGroup.EAR, 39f, 55f, path {
                moveTo(40f, 56f); cubicTo(37f, 52f, 36f, 50f, 36f, 48f); cubicTo(39f, 49f, 41f, 51f, 42f, 54f)
            }),
            Part(PartGroup.EAR, 60f, 55f, path {
                moveTo(59f, 56f); cubicTo(62f, 52f, 63f, 50f, 63f, 48f); cubicTo(60f, 49f, 58f, 51f, 57f, 54f)
            }),
            Part(PartGroup.BODY, 50f, 56f, path {   // splayed limbs
                moveTo(34f, 66f); cubicTo(30f, 70f, 28f, 72f, 26f, 72f)
                moveTo(66f, 66f); cubicTo(70f, 70f, 72f, 72f, 74f, 72f)
                moveTo(30f, 58f); cubicTo(24f, 60f, 22f, 62f, 22f, 64f)
                moveTo(70f, 58f); cubicTo(76f, 60f, 78f, 62f, 78f, 64f)
            }),
            Part(PartGroup.TAIL, 70f, 50f, path {
                moveTo(70f, 50f)
                cubicTo(78f, 46f, 84f, 48f, 86f, 54f)
                cubicTo(87f, 58f, 84f, 62f, 80f, 62f)
            }),
            Part(PartGroup.FACE, 50f, 62f, path {
                moveTo(43f, 62f); cubicTo(44.5f, 64f, 46.5f, 64f, 48f, 62f)
                moveTo(52f, 62f); cubicTo(53.5f, 64f, 55.5f, 64f, 57f, 62f)
            }),
            Part(PartGroup.FACE, 50f, 62f, path {
                moveTo(45.5f, 62.5f); lineTo(45.55f, 62.5f)
                moveTo(54.5f, 62.5f); lineTo(54.55f, 62.5f)
            }),
            Part(PartGroup.FACE, 50f, 62f, path {
                moveTo(48.5f, 66.5f); cubicTo(49.5f, 67.5f, 50.5f, 67.5f, 51.5f, 66.5f)
            })
        )
    )

    /* ---------------- Awake: the reference stretch ---------------- */
    val STRETCH = CatPose(
        name = "stretch", unitsW = 80f, unitsH = 96f,
        bodyPivotX = 46f, bodyPivotY = 60f, headAnchorX = 54f, headAnchorY = 34f,
        parts = listOf(
            Part(PartGroup.TAIL, 39f, 45f, path {
                moveTo(30f, 22f)
                cubicTo(28f, 16f, 34f, 12f, 40f, 14f)
                cubicTo(48f, 17f, 50f, 26f, 46f, 33f)
                cubicTo(43f, 38f, 40f, 41f, 39f, 45f)
            }),
            Part(PartGroup.BODY, 46f, 60f, path {   // back + hind leg
                moveTo(39f, 45f)
                cubicTo(33f, 47f, 30f, 53f, 31f, 59f)
                cubicTo(31f, 63f, 33f, 66f, 33f, 70f)
                cubicTo(33f, 74f, 31f, 76f, 31f, 78f)
                cubicTo(31f, 80f, 33f, 81f, 34f, 80f)
            }),
            Part(PartGroup.BODY, 46f, 60f, path {   // extended front legs
                moveTo(44f, 64f); cubicTo(45f, 72f, 47f, 80f, 49f, 86f); cubicTo(50f, 89f, 52f, 90f, 53f, 88f)
                moveTo(50f, 62f); cubicTo(52f, 70f, 54f, 78f, 56f, 84f); cubicTo(57f, 87f, 59f, 88f, 60f, 86f)
            }),
            Part(PartGroup.HEAD, 54f, 54f, path {
                moveTo(44f, 50f)
                cubicTo(46f, 44f, 56f, 42f, 62f, 46f)
                cubicTo(68f, 50f, 68f, 58f, 63f, 62f)
                cubicTo(58f, 66f, 49f, 66f, 46f, 61f)
                cubicTo(44f, 58f, 43f, 53f, 44f, 50f)
            }),
            Part(PartGroup.EAR, 47f, 46f, path {
                moveTo(45f, 47f); cubicTo(44f, 42f, 44f, 40f, 45f, 38f); cubicTo(48f, 40f, 49f, 42f, 50f, 45f)
            }),
            Part(PartGroup.EAR, 61f, 44f, path {
                moveTo(59f, 44f); cubicTo(61f, 39f, 62f, 37f, 64f, 36f); cubicTo(65f, 39f, 64f, 42f, 63f, 45f)
            }),
            Part(PartGroup.FACE, 54f, 54f, path {   // whiskers
                moveTo(44f, 56f); lineTo(38f, 55f)
                moveTo(44f, 58f); lineTo(38f, 59f)
                moveTo(64f, 55f); lineTo(70f, 54f)
                moveTo(64f, 57f); lineTo(70f, 58f)
            }),
            Part(PartGroup.FACE, 54f, 54f, path {   // closed eyes
                moveTo(49f, 54f); cubicTo(50.5f, 56f, 52.5f, 56f, 54f, 54f)
                moveTo(56f, 53f); cubicTo(57.5f, 55f, 59.5f, 55f, 61f, 53f)
            }),
            Part(PartGroup.FACE, 54f, 54f, path {   // open eyes
                moveTo(51f, 54f); lineTo(51.05f, 54f)
                moveTo(58f, 53f); lineTo(58.05f, 53f)
            }),
            Part(PartGroup.FACE, 54f, 54f, path {
                moveTo(54f, 58.5f); cubicTo(55f, 59.5f, 56f, 59.5f, 57f, 58.5f)
            })
        )
    )

    /* ---------------- Awake: low scamper walk ---------------- */
    val WALK = CatPose(
        name = "walk", unitsW = 92f, unitsH = 78f,
        bodyPivotX = 48f, bodyPivotY = 50f, headAnchorX = 76f, headAnchorY = 24f,
        parts = listOf(
            Part(PartGroup.BODY, 48f, 50f, path {
                moveTo(24f, 48f)
                cubicTo(30f, 42f, 44f, 40f, 54f, 42f)
                cubicTo(62f, 43f, 68f, 46f, 72f, 50f)
            }),
            Part(PartGroup.BODY, 48f, 50f, path {   // belly
                moveTo(28f, 58f); cubicTo(36f, 62f, 52f, 62f, 62f, 58f)
            }),
            Part(PartGroup.HEAD, 76f, 41f, path {
                moveTo(66f, 42f)
                cubicTo(66f, 35f, 74f, 31f, 80f, 34f)
                cubicTo(86f, 37f, 87f, 44f, 83f, 48f)
                cubicTo(79f, 52f, 71f, 52f, 68f, 48f)
                cubicTo(66f, 46f, 66f, 44f, 66f, 42f)
            }),
            Part(PartGroup.EAR, 70f, 34f, path {
                moveTo(68f, 35f); cubicTo(67f, 30f, 67f, 28f, 68f, 26f); cubicTo(71f, 28f, 72f, 30f, 73f, 33f)
            }),
            Part(PartGroup.EAR, 81f, 32f, path {
                moveTo(79f, 32f); cubicTo(80f, 27f, 81f, 25f, 83f, 24f); cubicTo(84f, 27f, 84f, 30f, 83f, 33f)
            }),
            Part(PartGroup.BODY, 48f, 50f, path {   // four stepping legs
                moveTo(64f, 56f); cubicTo(64f, 62f, 66f, 68f, 68f, 72f)
                moveTo(58f, 58f); cubicTo(58f, 64f, 57f, 70f, 56f, 74f)
                moveTo(30f, 56f); cubicTo(28f, 62f, 27f, 68f, 27f, 72f)
                moveTo(38f, 58f); cubicTo(38f, 64f, 39f, 68f, 40f, 72f)
            }),
            Part(PartGroup.TAIL, 24f, 48f, path {
                moveTo(24f, 48f)
                cubicTo(18f, 42f, 16f, 34f, 20f, 28f)
                cubicTo(22f, 25f, 26f, 24f, 28f, 26f)
            }),
            Part(PartGroup.FACE, 76f, 41f, path {   // closed eyes
                moveTo(72f, 41f); cubicTo(73.5f, 43f, 75.5f, 43f, 77f, 41f)
                moveTo(78f, 40f); cubicTo(79.5f, 42f, 81.5f, 42f, 83f, 40f)
            }),
            Part(PartGroup.FACE, 76f, 41f, path {   // open eyes
                moveTo(74.5f, 41.5f); lineTo(74.55f, 41.5f)
                moveTo(80.5f, 40.5f); lineTo(80.55f, 40.5f)
            }),
            Part(PartGroup.FACE, 76f, 41f, path {
                moveTo(76f, 45.5f); cubicTo(77f, 46.5f, 78f, 46.5f, 79f, 45.5f)
            })
        )
    )

    val SLEEP_POSES: List<CatPose> = listOf(CURLED, LOAF, SIDE, SPRAWL)
}
