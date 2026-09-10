package com.fde.gallery3d

import android.app.Activity
import android.app.ActivityManager
import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowInsetsController
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView


class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private var root: View? = null
    private var captionBar: View? = null
    private var materialToolbar: MaterialToolbar? = null

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        root = findViewById(R.id.activity_container)
        captionBar = findViewById(R.id.caption_bar)
        materialToolbar = findViewById(R.id.materialToolbar)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.visibility = View.GONE

        materialToolbar?.setTitle(R.string.app_name)
        materialToolbar?.setTitleTextAppearance(this, R.style.app_title)
        setTitle(R.string.app_name)


//        if (!Environment.isExternalStorageManager()) {
//            val intent = Intent(
//                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
//            )
//            intent.setData(Uri.parse("package:" + getPackageName()))
//            startActivity(intent)
//        }

        triggerSystemMediaScan();

        setupTransparentCaptionBar();
        setupCaptionBarInsets();
        setupGestureExclusion();

        val navHostFragment =
            (supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment?)!!
        val navController = navHostFragment.navController


        val navView = findViewById<NavigationView>(R.id.nav_view)
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)

        if (navView != null) {
            appBarConfiguration = AppBarConfiguration(
                setOf(
                    R.id.nav_picture,
                    R.id.nav_video,
                    R.id.nav_timeline,
                    R.id.nav_settings
                ),
                drawerLayout
            )

            setupActionBarWithNavController(navController, appBarConfiguration)
            navView.setupWithNavController(navController)
        }

        val bottomNavView =
            findViewById<BottomNavigationView>(R.id.bottom_nav_view)

        if (bottomNavView != null) {
            appBarConfiguration = AppBarConfiguration(
                setOf(
                    R.id.nav_picture,
                    R.id.nav_video,
                    R.id.nav_timeline
                )
            )

            setupActionBarWithNavController(navController, appBarConfiguration)
            bottomNavView.setupWithNavController(navController)
        }


    }

    fun triggerSystemMediaScan() {
        val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
        val externalDir = Environment.getExternalStorageDirectory()
        val contentUri = Uri.fromFile(externalDir)
        mediaScanIntent.setData(contentUri)
        sendBroadcast(mediaScanIntent)
    }


    @RequiresApi(Build.VERSION_CODES.R)
    private fun setupTransparentCaptionBar() {
        window.insetsController?.setSystemBarsAppearance(
            WindowInsetsController.APPEARANCE_TRANSPARENT_CAPTION_BAR_BACKGROUND,
            WindowInsetsController.APPEARANCE_TRANSPARENT_CAPTION_BAR_BACKGROUND
        )
//        captionBar?.setBackgroundColor(
//            ContextCompat.getColor(this, R.color.bg_circle_dark)
//        )

//        captionBar?.setTextColor(
//            ContextCompat.getColor(this, R.color.bg_circle_dark)
//        )
    }

    private fun setupCaptionBarInsets() {
        root?.let { rootView ->
            ViewCompat.setOnApplyWindowInsetsListener(rootView) { _, windowInsets ->
                captionBar?.setPadding(0, 0, 0, 0)
                windowInsets
            }

            ViewCompat.requestApplyInsets(rootView)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun setupGestureExclusion() {
        val decorView = window.decorView

        val updateGestureExclusion = {
            if (decorView.width > 0) {
                val height = (48 * resources.displayMetrics.density).toInt()

                decorView.systemGestureExclusionRects = listOf(
                    Rect(
                        decorView.width -200 ,
                        0,
                        decorView.width,
                        height
                    )
                )
            }
        }

        updateGestureExclusion()

        decorView.viewTreeObserver.addOnGlobalLayoutListener {
            updateGestureExclusion()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val result = super.onCreateOptionsMenu(menu)
        // Using findViewById because NavigationView exists in different layout files
        // between w600dp and w1240dp
        val navView: NavigationView? = findViewById(R.id.nav_view)
        if (navView == null) {
            // The navigation drawer already has the items including the items in the overflow menu
            // We only inflate the overflow menu if the navigation drawer isn't visible
            menuInflater.inflate(R.menu.settings, menu)
        }
        return result
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_settings -> {
                val navController = findNavController(R.id.nav_host_fragment_content_main)
                navController.navigate(R.id.nav_settings)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}