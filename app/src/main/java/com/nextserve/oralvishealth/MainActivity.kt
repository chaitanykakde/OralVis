package com.nextserve.oralvishealth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import android.widget.TextView
import android.view.View
import android.widget.LinearLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.nextserve.oralvishealth.databinding.ActivityMainBinding
import com.nextserve.oralvishealth.ui.auth.LoginActivity

// Main activity - this is where everything starts after login
// handles the bottom navigation and user authentication stuff
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // setup the view binding - much cleaner than findViewById
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // initialize firebase auth
        auth = FirebaseAuth.getInstance()

        // Remove toolbar setup since we're using custom app bar

        val navView: BottomNavigationView = binding.bottomNavigation

        // setup navigation controller for bottom nav
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.navHostFragment) as androidx.navigation.fragment.NavHostFragment
        val navController = navHostFragment.navController
        
        // setup bottom navigation with nav controller
        navView.setupWithNavController(navController)

        // setup custom app bar profile icon click to open navigation drawer
        val customAppBar = binding.root.findViewById<LinearLayout>(R.id.customAppBar)
        val profileIcon = customAppBar.findViewById<de.hdodenhof.circleimageview.CircleImageView>(R.id.ivAppBarProfile)
        profileIcon.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.END)
        }

        // setup navigation drawer menu item clicks
        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    binding.bottomNavigation.selectedItemId = R.id.nav_sessions
                }
                R.id.nav_feedback -> {
                    // Handle feedback action
                }
                R.id.nav_logout -> {
                    logout()
                }
            }
            binding.drawerLayout.closeDrawer(GravityCompat.END)
            true
        }

        // first thing - check if user is actually logged in
        // if not, kick them back to login screen
        if (FirebaseAuth.getInstance().currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // user is logged in, so load their profile data
        loadUserProfile()
        
        // Handle window insets for edge-to-edge display
        setupWindowInsets()
    }

    // load user profile info from firebase auth
    // this gets called after we confirm user is logged in
    private fun loadUserProfile() {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            // fallback to "User" if no display name set
            val displayName = user.displayName ?: "User"
            val customAppBar = binding.root.findViewById<LinearLayout>(R.id.customAppBar)
            val greetingText = customAppBar.findViewById<TextView>(R.id.tvAppBarTitle)
            greetingText.text = getString(R.string.hello_user, displayName)
            
            // setup navigation drawer header with user info
            val headerView = binding.navigationView.getHeaderView(0)
            val tvNavUserName = headerView.findViewById<android.widget.TextView>(R.id.tvNavUserName)
            val tvNavUserEmail = headerView.findViewById<android.widget.TextView>(R.id.tvNavUserEmail)
            val ivNavProfile = headerView.findViewById<de.hdodenhof.circleimageview.CircleImageView>(R.id.ivNavProfile)
            
            tvNavUserName.text = displayName
            tvNavUserEmail.text = user.email
            
            // load profile picture if available, otherwise use app logo
            val photoUrl = user.photoUrl
            if (photoUrl != null) {
                Glide.with(this@MainActivity)
                    .load(photoUrl)
                    .placeholder(R.drawable.app_logo)
                    .error(R.drawable.app_logo)
                    .into(ivNavProfile)
            } else {
                ivNavProfile.setImageResource(R.drawable.app_logo)
            }
            
            // also load profile image in custom app bar
            val appBarProfileIcon = customAppBar.findViewById<de.hdodenhof.circleimageview.CircleImageView>(R.id.ivAppBarProfile)
            if (photoUrl != null) {
                Glide.with(this@MainActivity)
                    .load(photoUrl)
                    .placeholder(R.drawable.app_logo)
                    .error(R.drawable.app_logo)
                    .into(appBarProfileIcon)
            } else {
                appBarProfileIcon.setImageResource(R.drawable.app_logo)
            }
        }
    }

    // simple logout function - clears everything and goes back to login
    private fun logout() {
        auth.signOut()
        val intent = Intent(this, LoginActivity::class.java)
        // clear the entire task stack so user can't go back
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    // Setup window insets to prevent custom app bar from overlapping with status bar
    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            // Apply top padding to custom app bar status bar spacer
            val customAppBar = binding.root.findViewById<LinearLayout>(R.id.customAppBar)
            val statusBarSpacer = customAppBar.findViewById<View>(R.id.statusBarSpacer)
            statusBarSpacer.layoutParams.height = systemBars.top
            statusBarSpacer.requestLayout()
            
            // Apply bottom padding to bottom navigation for navigation bar
            binding.bottomNavigation.updatePadding(bottom = systemBars.bottom)
            
            insets
        }
    }

    // handle back button - close drawer first if its open
    @Deprecated("This method has been deprecated in favor of using the OnBackPressedDispatcher")
    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.END)) {
            binding.drawerLayout.closeDrawer(GravityCompat.END)
        } else {
            super.onBackPressed()
        }
    }
}