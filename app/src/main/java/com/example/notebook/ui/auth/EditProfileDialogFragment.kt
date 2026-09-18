package com.example.notebook

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.notebook.databinding.DialogEditProfileBinding
import com.example.notebook.util.FirebaseUtil
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import java.io.ByteArrayOutputStream
import java.util.Locale

class EditProfileDialogFragment(
    private val currentName: String,
    private val currentUsername: String,
    private val currentPronouns: String,
    private val currentBio: String,
    private val currentStyle: String,
    private val currentCoverColor: String?,
    private val currentAvatarBase64: String?,
    private val onProfileUpdated: () -> Unit
) : DialogFragment() {

    private var _binding: DialogEditProfileBinding? = null
    private val binding get() = _binding!!

    private var selectedImageBase64: String? = null
    private var selectedStyleId: String = currentStyle
    private var selectedColorHex: String? = currentCoverColor

    private val swatchColors = listOf(
        "#FDF6ED", "#EAF1F8", "#E8D9C0", "#F3E9F7", "#FBE3DB", "#E4EDE4"
    )
    private val swatchViews = mutableListOf<View>()

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            handlePickedImage(uri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.inputEditName.setText(currentName)
        binding.inputEditUsername.setText(currentUsername)
        binding.inputEditBio.setText(currentBio)

        if (!currentAvatarBase64.isNullOrEmpty()) {
            setAvatarFromBase64(currentAvatarBase64)
        }

        val pronounOptions = listOf("She/her", "He/him", "They/them", "Prefer not to say")
        binding.inputEditPronouns.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, pronounOptions)
        )
        binding.inputEditPronouns.setText(currentPronouns, false)

        binding.imageEditAvatar.setOnClickListener { launchPicker() }
        binding.buttonChangeAvatar.setOnClickListener { launchPicker() }

        setupStylePicker()
        setupColorSwatches()

        binding.buttonSaveProfile.setOnClickListener { onSaveClicked() }
    }

    private fun launchPicker() {
        pickMedia.launch(
            androidx.activity.result.PickVisualMediaRequest(
                ActivityResultContracts.PickVisualMedia.ImageOnly
            )
        )
    }

    private fun handlePickedImage(uri: Uri) {
        val encoded = encodeImageToBase64(uri)
        if (encoded == null) {
            showError("Couldn't process that image, try another")
            return
        }
        selectedImageBase64 = encoded
        setAvatarFromBase64(encoded)
    }

    private fun encodeImageToBase64(uri: Uri): String? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val original = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            if (original == null) return null

            val size = 240
            val scaled = Bitmap.createScaledBitmap(original, size, size, true)

            val outputStream = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            val bytes = outputStream.toByteArray()

            Base64.encodeToString(bytes, Base64.DEFAULT)
        } catch (e: Exception) {
            null
        }
    }

    private fun setAvatarFromBase64(base64: String) {
        try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            binding.imageEditAvatar.setImageBitmap(bitmap)
        } catch (e: Exception) {
            // Leave placeholder if decoding fails
        }
    }

    private fun setupStylePicker() {
        val styles = listOf(
            NotebookStyleOption(id = "classic", label = "Classic", previewIconRes = R.drawable.ic_pattern_classic),
            NotebookStyleOption(id = "grid", label = "Grid", previewIconRes = R.drawable.ic_pattern_grid),
            NotebookStyleOption(id = "kraft", label = "Kraft", previewIconRes = R.drawable.ic_pattern_kraft),
            NotebookStyleOption(id = "dotted", label = "Dotted", previewIconRes = R.drawable.ic_pattern_dotted)
        )

        val adapter = NotebookStyleAdapter(styles, initialSelectedId = selectedStyleId) { selected ->
            selectedStyleId = selected.id
        }

        binding.recyclerEditNotebookStyles.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerEditNotebookStyles.adapter = adapter
    }

    private fun setupColorSwatches() {
        binding.containerColorSwatches.removeAllViews()
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
            val circleDrawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor(hex))
            }
            circle.background = circleDrawable
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
            frame.tag = hex

            frame.setOnClickListener {
                selectedColorHex = hex
                swatchViews.forEach { v -> (v as FrameLayout).getChildAt(1).visibility = View.GONE }
                (it as FrameLayout).getChildAt(1).visibility = View.VISIBLE
            }

            binding.containerColorSwatches.addView(frame)
            swatchViews.add(frame)
        }
    }

    private fun onSaveClicked() {
        val newName = binding.inputEditName.text?.toString()?.trim().orEmpty()
        val newUsername = binding.inputEditUsername.text?.toString()?.trim().orEmpty()
        val newPronouns = binding.inputEditPronouns.text?.toString()?.trim().orEmpty()
        val newBio = binding.inputEditBio.text?.toString()?.trim().orEmpty()

        if (newName.isEmpty()) {
            showError("Please enter a display name")
            return
        }
        if (newUsername.isEmpty()) {
            showError("Please choose a username")
            return
        }

        binding.buttonSaveProfile.isEnabled = false

        if (newUsername.equals(currentUsername, ignoreCase = true)) {
            saveChanges(newName, newUsername, newPronouns, newBio)
        } else {
            checkUsernameAvailable(newUsername) { isAvailable ->
                if (isAvailable) {
                    saveChanges(newName, newUsername, newPronouns, newBio)
                } else {
                    binding.buttonSaveProfile.isEnabled = true
                    showError("That username is already taken")
                }
            }
        }
    }

    private fun checkUsernameAvailable(username: String, onResult: (Boolean) -> Unit) {
        val usernameLower = username.lowercase(Locale.getDefault())

        FirebaseUtil.database.getReference("users")
            .orderByChild("usernameLower")
            .equalTo(usernameLower)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    onResult(!snapshot.exists())
                }

                override fun onCancelled(error: DatabaseError) {
                    binding.buttonSaveProfile.isEnabled = true
                    showError("Couldn't verify username: ${error.message}")
                }
            })
    }

    private fun saveChanges(name: String, username: String, pronouns: String, bio: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            binding.buttonSaveProfile.isEnabled = true
            showError("Session expired. Please log in again.")
            return
        }

        val updates = mutableMapOf<String, Any?>(
            "displayName" to name,
            "username" to username,
            "usernameLower" to username.lowercase(Locale.getDefault()),
            "pronouns" to pronouns,
            "bio" to bio,
            "notebookStyle" to selectedStyleId,
            "coverColor" to selectedColorHex
        )
        if (selectedImageBase64 != null) {
            updates["avatarBase64"] = selectedImageBase64
        }

        FirebaseUtil.database.getReference("users").child(uid)
            .updateChildren(updates)
            .addOnSuccessListener {
                onProfileUpdated()
                dismiss()
            }
            .addOnFailureListener { e ->
                binding.buttonSaveProfile.isEnabled = true
                showError("Couldn't save changes: ${e.message}")
            }
    }

    private fun showError(message: String) {
        binding.textEditError.text = message
        binding.textEditError.visibility = View.VISIBLE
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}