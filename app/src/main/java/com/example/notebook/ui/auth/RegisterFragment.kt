package com.example.notebook.ui.auth

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.notebook.R
import com.example.notebook.databinding.FragmentRegisterBinding

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonRegister.setOnClickListener {
            val name = binding.inputName.text.toString().trim()
            val email = binding.inputEmail.text.toString().trim()
            val password = binding.inputPassword.text.toString().trim()
            val confirm = binding.inputConfirmPassword.text.toString().trim()

            when {
                name.isEmpty() -> showError("Enter your name")
                !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> showError("Enter a valid email")
                password.length < 6 -> showError("Password must be at least 6 characters")
                password != confirm -> showError("Passwords don't match")
                else -> {
                    binding.textError.visibility = View.GONE
                    // Firebase account creation goes here in the auth phase
                }
            }
        }

        binding.textGoLogin.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_profileSetupFragment)
        }
    }

    private fun showError(message: String) {
        binding.textError.text = message
        binding.textError.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}