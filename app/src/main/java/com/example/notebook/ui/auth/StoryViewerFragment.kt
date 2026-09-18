package com.example.notebook

import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.notebook.databinding.FragmentStoryViewerBinding
import com.example.notebook.util.FirebaseUtil
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class StoryViewerFragment : Fragment() {

    private var _binding: FragmentStoryViewerBinding? = null
    private val binding get() = _binding!!

    private var authorUids: List<String> = emptyList()
    private var authors: List<StoryAuthor> = emptyList()

    private var authorIndex = 0
    private var noteIndex = 0

    private var segmentAnimator: ValueAnimator? = null
    private val segmentDurationMs = 5000L

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStoryViewerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        authorUids = arguments?.getStringArray("authorUids")?.toList().orEmpty()
        authorIndex = arguments?.getInt("startIndex", 0) ?: 0

        binding.buttonCloseStory.setOnClickListener { findNavController().popBackStack() }

        binding.tapZoneLeft.layoutParams = (binding.tapZoneLeft.layoutParams as ViewGroup.LayoutParams)
        binding.tapZoneLeft.setOnClickListener { goToPrevious() }
        binding.tapZoneRight.setOnClickListener { goToNext() }

        // give tap zones actual width via weight-like split (30% / 70%) after layout
        view.post {
            val totalWidth = view.width
            binding.tapZoneLeft.layoutParams = binding.tapZoneLeft.layoutParams.apply {
                width = (totalWidth * 0.3f).toInt()
            }
            binding.tapZoneRight.layoutParams = binding.tapZoneRight.layoutParams.apply {
                width = (totalWidth * 0.7f).toInt()
            }
            binding.tapZoneLeft.requestLayout()
            binding.tapZoneRight.requestLayout()
        }

        loadAllAuthors()
    }

    private fun loadAllAuthors() {
        if (authorUids.isEmpty()) {
            findNavController().popBackStack()
            return
        }

        val now = System.currentTimeMillis()
        var remaining = authorUids.size
        val loaded = mutableListOf<StoryAuthor>()

        authorUids.forEach { uid ->
            FirebaseUtil.database.getReference("dailyNotes")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val notes = snapshot.children
                            .mapNotNull { it.getValue(DailyNote::class.java) }
                            .filter { it.authorUid == uid && it.expiresAt > now }
                            .sortedBy { it.createdAt }

                        if (notes.isNotEmpty()) {
                            FirebaseUtil.database.getReference("users").child(uid)
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(userSnapshot: DataSnapshot) {
                                        loaded.add(
                                            StoryAuthor(
                                                uid = uid,
                                                username = userSnapshot.child("username").getValue(String::class.java).orEmpty(),
                                                avatarBase64 = userSnapshot.child("avatarBase64").getValue(String::class.java),
                                                dailyNotes = notes
                                            )
                                        )
                                        remaining--
                                        if (remaining == 0) onAuthorsLoaded(loaded)
                                    }

                                    override fun onCancelled(error: DatabaseError) {
                                        remaining--
                                        if (remaining == 0) onAuthorsLoaded(loaded)
                                    }
                                })
                        } else {
                            remaining--
                            if (remaining == 0) onAuthorsLoaded(loaded)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        remaining--
                        if (remaining == 0) onAuthorsLoaded(loaded)
                    }
                })
        }
    }

    private fun onAuthorsLoaded(loaded: List<StoryAuthor>) {
        if (_binding == null) return

        // preserve original tap order from the stories row
        authors = authorUids.mapNotNull { uid -> loaded.find { it.uid == uid } }

        if (authors.isEmpty()) {
            findNavController().popBackStack()
            return
        }

        if (authorIndex >= authors.size) authorIndex = authors.size - 1
        noteIndex = 0
        buildProgressBars()
        showCurrentNote()
    }

    private fun buildProgressBars() {
        binding.containerProgressBars.removeAllViews()
        val author = authors[authorIndex]

        author.dailyNotes.forEachIndexed { index, _ ->
            val track = FrameLayoutTrack(requireContext())
            val params = android.widget.LinearLayout.LayoutParams(0, dp(3)).apply {
                weight = 1f
                marginStart = dp(2)
                marginEnd = dp(2)
            }
            track.layoutParams = params
            track.setBackgroundColor(adjustAlpha(Color.WHITE, 0.35f))
            binding.containerProgressBars.addView(track)
        }
    }

    private fun showCurrentNote() {
        val author = authors[authorIndex]
        val note = author.dailyNotes[noteIndex]

        binding.textStoryAuthorUsername.text = author.username
        binding.textStoryContent.text = note.content

        try {
            binding.root.setBackgroundColor(Color.parseColor(note.colorHex))
        } catch (e: IllegalArgumentException) { /* keep default */ }

        val patternRes = when (note.template) {
            "grid" -> R.drawable.ic_pattern_grid
            "kraft" -> R.drawable.ic_pattern_kraft
            "dotted" -> R.drawable.ic_pattern_dotted
            else -> R.drawable.ic_pattern_classic
        }
        binding.imageStoryPattern.setImageResource(patternRes)

        animateCurrentSegment()
    }

    private fun animateCurrentSegment() {
        segmentAnimator?.cancel()

        val track = binding.containerProgressBars.getChildAt(noteIndex) as? FrameLayoutTrack ?: return
        track.setFillFraction(0f)

        segmentAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = segmentDurationMs
            addUpdateListener { anim ->
                track.setFillFraction(anim.animatedValue as Float)
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    goToNext()
                }
            })
            start()
        }
    }

    private fun goToNext() {
        val author = authors.getOrNull(authorIndex) ?: return

        if (noteIndex < author.dailyNotes.size - 1) {
            markSegmentComplete(noteIndex)
            noteIndex++
            showCurrentNote()
        } else if (authorIndex < authors.size - 1) {
            authorIndex++
            noteIndex = 0
            buildProgressBars()
            showCurrentNote()
        } else {
            segmentAnimator?.cancel()
            findNavController().popBackStack()
        }
    }

    private fun goToPrevious() {
        if (noteIndex > 0) {
            noteIndex--
            showCurrentNote()
        } else if (authorIndex > 0) {
            authorIndex--
            noteIndex = authors[authorIndex].dailyNotes.size - 1
            buildProgressBars()
            showCurrentNote()
        } else {
            showCurrentNote() // restart first segment
        }
    }

    private fun markSegmentComplete(index: Int) {
        val track = binding.containerProgressBars.getChildAt(index) as? FrameLayoutTrack
        track?.setFillFraction(1f)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun adjustAlpha(color: Int, factor: Float): Int {
        val alpha = (Color.alpha(color) * factor).toInt()
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        segmentAnimator?.cancel()
        _binding = null
    }
}

/**
 * A tiny custom View used only for the progress bar segments.
 * Draws a background track plus a white fill that grows left-to-right.
 */
private class FrameLayoutTrack(context: android.content.Context) : View(context) {
    private var fillFraction = 0f
    private val fillPaint = android.graphics.Paint().apply {
        color = Color.WHITE
        style = android.graphics.Paint.Style.FILL
    }

    fun setFillFraction(fraction: Float) {
        fillFraction = fraction.coerceIn(0f, 1f)
        invalidate()
    }

    override fun onDraw(canvas: android.graphics.Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width * fillFraction, height.toFloat(), fillPaint)
    }
}