package com.example.notebook

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.notebook.databinding.FragmentNoteEditorBinding
import com.example.notebook.util.FirebaseUtil
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NoteEditorFragment : Fragment() {

    private var _binding: FragmentNoteEditorBinding? = null
    private val binding get() = _binding!!

    private lateinit var templateAdapter: NotebookStyleAdapter
    private var selectedTemplate = "classic"
    private var selectedColorHex = "#FDF6ED"
    private var isAnonymous = false
    private var isDaily = false

    private val swatchColors = listOf(
        "#FDF6ED", "#EAF1F8", "#E8D9C0", "#F3E9F7", "#FBE3DB", "#E4EDE4"
    )
    private val swatchViews = mutableListOf<View>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNoteEditorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        isAnonymous = arguments?.getBoolean("isAnonymous", false) ?: false
        isDaily = arguments?.getBoolean("isDaily", false) ?: false
        binding.textEditorLabel.text = when {
            isDaily -> "Daily Note"
            isAnonymous -> "Anonymous Note"
            else -> "New Note"
        }

        binding.textDate.text = SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date())
        binding.textNoteDateStamp.text = SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date())

        binding.buttonBack.setOnClickListener { findNavController().popBackStack() }

        setupTemplatePicker()
        setupColorSwatches()
        applyTemplate(selectedTemplate)
        applyPageColor(selectedColorHex)

        binding.buttonPost.setOnClickListener { postNote() }
    }

    private fun setupTemplatePicker() {
        val templates = listOf(
            NotebookStyleOption(id = "classic", label = "Classic", previewIconRes = R.drawable.ic_pattern_classic),
            NotebookStyleOption(id = "grid", label = "Grid", previewIconRes = R.drawable.ic_pattern_grid),
            NotebookStyleOption(id = "kraft", label = "Kraft", previewIconRes = R.drawable.ic_pattern_kraft),
            NotebookStyleOption(id = "dotted", label = "Dotted", previewIconRes = R.drawable.ic_pattern_dotted)
        )

        templateAdapter = NotebookStyleAdapter(templates, initialSelectedId = selectedTemplate) { selected ->
            selectedTemplate = selected.id
            applyTemplate(selectedTemplate)
        }

        binding.recyclerTemplates.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerTemplates.adapter = templateAdapter
    }

    private fun applyTemplate(templateId: String) {
        val patternRes = when (templateId) {
            "grid" -> R.drawable.ic_pattern_grid
            "kraft" -> R.drawable.ic_pattern_kraft
            "dotted" -> R.drawable.ic_pattern_dotted
            else -> R.drawable.ic_pattern_classic
        }
        binding.imageNotePattern.setImageResource(patternRes)
    }

    private fun setupColorSwatches() {
        binding.containerColors.removeAllViews()
        swatchViews.clear()

        val sizePx = (40 * resources.displayMetrics.density).toInt()
        val marginPx = (8 * resources.displayMetrics.density).toInt()

        swatchColors.forEach { hex ->
            val frame = FrameLayout(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(sizePx, sizePx).apply {
                    marginStart = marginPx
                    marginEnd = marginPx
                }
            }

            val circle = View(requireContext())
            circle.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor(hex))
            }
            circle.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )

            val ring = View(requireContext())
            ring.background = ContextCompat.getDrawable(requireContext(), R.drawable.swatch_selected_ring)
            ring.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            ring.visibility = if (hex.equals(selectedColorHex, ignoreCase = true)) View.VISIBLE else View.GONE

            frame.addView(circle)
            frame.addView(ring)

            frame.setOnClickListener {
                selectedColorHex = hex
                swatchViews.forEach { v -> (v as FrameLayout).getChildAt(1).visibility = View.GONE }
                (it as FrameLayout).getChildAt(1).visibility = View.VISIBLE
                applyPageColor(hex)
            }

            binding.containerColors.addView(frame)
            swatchViews.add(frame)
        }
    }

    private fun applyPageColor(hex: String) {
        try {
            binding.cardWritingArea.setCardBackgroundColor(Color.parseColor(hex))
        } catch (e: IllegalArgumentException) { /* ignore */ }
    }

    private fun postNote() {
        val content = binding.inputNoteContent.text?.toString()?.trim().orEmpty()
        if (content.isEmpty()) {
            binding.inputNoteContent.error = "Write something first"
            return
        }

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        binding.buttonPost.isEnabled = false

        if (isDaily) {
            postDailyNote(uid, content)
        } else {
            postRegularNote(uid, content)
        }
    }

    private fun postRegularNote(uid: String, content: String) {
        val noteId = FirebaseUtil.database.getReference("notes").push().key ?: return
        val dateDisplay = SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date())

        val note = Note(
            id = noteId,
            authorUid = uid,
            anonymous = isAnonymous,
            content = content,
            template = selectedTemplate,
            colorHex = selectedColorHex,
            dateDisplay = dateDisplay,
            createdAt = System.currentTimeMillis()
        )

        val updates = mapOf(
            "/notes/$noteId" to note,
            "/userNotes/$uid/$noteId" to note
        )

        FirebaseUtil.database.reference.updateChildren(updates)
            .addOnSuccessListener {
                binding.buttonPost.isEnabled = true
                findNavController().popBackStack(R.id.notesFeedFragment, false)
            }
            .addOnFailureListener {
                binding.buttonPost.isEnabled = true
                binding.inputNoteContent.error = "Couldn't post, try again"
            }
    }

    private fun postDailyNote(uid: String, content: String) {
        val noteId = FirebaseUtil.database.getReference("dailyNotes").push().key ?: return
        val now = System.currentTimeMillis()

        val dailyNote = DailyNote(
            id = noteId,
            authorUid = uid,
            content = content,
            template = selectedTemplate,
            colorHex = selectedColorHex,
            createdAt = now,
            expiresAt = now + (24 * 60 * 60 * 1000L)
        )

        FirebaseUtil.database.getReference("dailyNotes").child(noteId)
            .setValue(dailyNote)
            .addOnSuccessListener {
                binding.buttonPost.isEnabled = true
                findNavController().popBackStack(R.id.notesFeedFragment, false)
            }
            .addOnFailureListener {
                binding.buttonPost.isEnabled = true
                binding.inputNoteContent.error = "Couldn't post, try again"
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}