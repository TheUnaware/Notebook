package com.example.notebook

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import com.example.notebook.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private val bottomNavHiddenDestinations = setOf(
        R.id.loginFragment,
        R.id.registerFragment,
        R.id.profileSetupFragment,
        R.id.storyViewerFragment
    )

    // The four real tabs. createNoteFragment is handled separately since
    // it's a one-off action screen, not a persistent tab.
    private val topLevelDestinations = setOf(
        R.id.notesFeedFragment,
        R.id.searchFragment,
        R.id.anonymousFragment,
        R.id.profileFragment
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        binding.bottomNav.setOnItemSelectedListener { item ->
            handleBottomNavClick(item.itemId, navController)
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomNav.visibility =
                if (destination.id in bottomNavHiddenDestinations) {
                    android.view.View.GONE
                } else {
                    android.view.View.VISIBLE
                }

            // Only touch the checked state for real tabs. Leave it alone for
            // createNoteFragment, noteDetailFragment, storyViewerFragment etc.
            val menu = binding.bottomNav.menu
            if (destination.id in topLevelDestinations) {
                menu.findItem(destination.id)?.isChecked = true
            }
        }
    }

    private fun handleBottomNavClick(itemId: Int, navController: NavController): Boolean {
        if (itemId == R.id.createNoteFragment) {
            navController.navigate(R.id.createNoteFragment)
            return false // not a persistent tab — don't mark it checked
        }

        if (itemId !in topLevelDestinations) return false

        // Already on this tab — do nothing instead of re-navigating.
        if (navController.currentDestination?.id == itemId) return true

        navController.navigate(
            itemId,
            null,
            navOptions {
                // Single consistent anchor for every tab: pop back to
                // notesFeedFragment (or, if that's the target itself, pop it
                // too so we always land on one fresh instance).
                popUpTo(R.id.notesFeedFragment) {
                    inclusive = (itemId == R.id.notesFeedFragment)
                }
                launchSingleTop = true
            }
        )
        return true
    }
}