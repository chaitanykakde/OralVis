package com.nextserve.oralvishealth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
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

        // setup toolbar as action bar
        setSupportActionBar(binding.toolbar)

        val navView: BottomNavigationView = binding.bottomNavigation

        // setup navigation controller for bottom nav
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.navHostFragment) as androidx.navigation.fragment.NavHostFragment
        val navController = navHostFragment.navController
        
        // these are the main tabs - home, cloud, profile
        // each one is a top level destination so no back arrow needed
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_sessions, R.id.nav_cloud
            ), binding.drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        
        // hide the default toolbar title since we have custom greeting
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // setup profile icon click to open navigation drawer
        binding.ivProfile.setOnClickListener {
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
    }

    // load user profile info from firebase auth
    // this gets called after we confirm user is logged in
    private fun loadUserProfile() {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            // fallback to "User" if no display name set
            val displayName = user.displayName ?: "User"
            binding.tvGreeting.text = getString(R.string.hello_user, displayName)
            
            // setup navigation drawer header with user info
            val headerView = binding.navigationView.getHeaderView(0)
            val tvNavUserName = headerView.findViewById<android.widget.TextView>(R.id.tvNavUserName)
            val tvNavUserEmail = headerView.findViewById<android.widget.TextView>(R.id.tvNavUserEmail)
            val ivNavProfile = headerView.findViewById<de.hdodenhof.circleimageview.CircleImageView>(R.id.ivNavProfile)
            
            tvNavUserName.text = displayName
            tvNavUserEmail.text = user.email
            
            // load profile picture if available, otherwise use app logo
            user.photoUrl?.let { photoUrl ->
                Glide.with(this)
                    .load(photoUrl)
                    .placeholder(R.drawable.app_logo)
                    .into(binding.ivProfile)
                
                // also set it in nav drawer
                Glide.with(this)
                    .load(photoUrl)
                    .placeholder(R.drawable.app_logo)
                    .into(ivNavProfile)
            } ?: run {
                // no profile photo, use app logo
                binding.ivProfile.setImageResource(R.drawable.app_logo)
                ivNavProfile.setImageResource(R.drawable.app_logo)
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