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
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowInsetsController
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavOptions
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.fde.gallery3d.ui.search.SearchState
import com.fde.gallery3d.ui.utils.LogTools
import com.fde.gallery3d.ui.view.SmartAlbumMenuView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView


class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private var root: View? = null
    private var captionBar: View? = null
    private var leftNavigator: View? = null
    private var layoutBrand: View? = null
    private var viewDivider: View? = null
    private var materialToolbar: MaterialToolbar? = null
    private var captionText: TextView? = null
    private var smartAlbumMenu: SmartAlbumMenuView? = null

    private val searchHandler = Handler(Looper.getMainLooper())

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        root = findViewById(R.id.activity_container)
        captionBar = findViewById(R.id.caption_bar)
        leftNavigator = findViewById(R.id.left_layout)
        viewDivider = findViewById(R.id.view_divider)
        layoutBrand = findViewById(R.id.layout_brand)
        materialToolbar = findViewById(R.id.materialToolbar)
        captionText = findViewById(R.id.caption_text)
        smartAlbumMenu = findViewById(R.id.smart_album_menu)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.visibility = View.GONE

        setCaptionTitle(getString(R.string.app_name))
        setTitle(R.string.app_name)

        val titleColor = ContextCompat.getColor(this, R.color.dark_text)
        materialToolbar?.setTitleTextColor(titleColor)

        triggerSystemMediaScan();

        setupTransparentCaptionBar();
        setupCaptionBarInsets();
        setupGestureExclusion();

        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { view, insets ->

            val systemBarsVisible =
                insets.isVisible(WindowInsetsCompat.Type.statusBars()) ||
                        insets.isVisible(WindowInsetsCompat.Type.navigationBars())

            val isFullscreen = !systemBarsVisible
            Log.d("Fullscreen", "isFullscreen=$isFullscreen")
            if(isFullscreen){
                leftNavigator?.visibility = View.VISIBLE
                viewDivider?.visibility = View.VISIBLE
                layoutBrand?.background = ContextCompat.getDrawable(this, R.color.side_bg)
            }else{
                leftNavigator?.visibility = View.GONE
                viewDivider?.visibility = View.GONE
                layoutBrand?.background = ContextCompat.getDrawable(this, R.color.app_white)
            }
            insets
        }

        val navHostFragment =
            (supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment?)!!
        val navController = navHostFragment.navController


        val navViewTop = findViewById<NavigationView>(R.id.nav_view)
        val navViewBottom = findViewById<NavigationView>(R.id.nav_view_bottom)
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)

        if (navViewTop != null || navViewBottom != null) {
            appBarConfiguration = AppBarConfiguration(
                setOf(
                    R.id.nav_picture,
                    R.id.nav_video,
                    R.id.nav_timeline,
                    R.id.nav_collection,
                    R.id.nav_category_portrait,
                    R.id.nav_category_landscape,
                    R.id.nav_category_animal,
                    R.id.nav_category_food,
                    R.id.nav_category_plant,
                    R.id.nav_category_architecture,
                    R.id.nav_category_vehicle,
                    R.id.nav_trash,
                    R.id.nav_search,
                    R.id.nav_settings
                ),
                drawerLayout
            )

            setupActionBarWithNavController(navController, appBarConfiguration)
            navViewTop?.setupWithNavController(navController)
            navViewBottom?.setupWithNavController(navController)
        }

        smartAlbumMenu?.let { albumMenu ->
            albumMenu.onCategorySelected = { destinationId ->
                // 分类目的地与抽屉顶级菜单同级：进入前先回弹到起始页，避免图册分类
                // 叠加在其它顶级页之上，导致再次点击图片/收藏时被 saveState/restoreState
                // 恢复成智能图册分类页。
                val navOptions = NavOptions.Builder()
                    .setLaunchSingleTop(true)
                    .setPopUpTo(
                        navController.graph.findStartDestination().id,
                        inclusive = false,
                        saveState = true
                    )
                    .build()
                navController.navigate(destinationId, null, navOptions)
                drawerLayout?.closeDrawer(GravityCompat.START, true)
            }
            navController.addOnDestinationChangedListener { _, destination, _ ->
                albumMenu.setChecked(destination.id)
            }
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

        setupSideSearch(navController)
        setupToolbarTitle(navController)
    }

    /**
     * 左侧搜索框：输入关键字后在右侧的搜索页面展示结果
     */
    private fun setupSideSearch(navController: NavController) {
        val editText = findViewById<EditText>(R.id.search_edit_text) ?: return
        val clear = findViewById<ImageView>(R.id.search_clear)

        clear?.setOnClickListener {
            editText.setText("")
            SearchState.getInstance().query = ""
            searchHandler.removeCallbacksAndMessages(null)
            if (navController.currentDestination?.id != R.id.nav_picture) {
                navController.popBackStack(R.id.nav_picture, false)
            }
        }

        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim().orEmpty()
                clear?.visibility = if (query.isEmpty()) View.GONE else View.VISIBLE
                searchHandler.removeCallbacksAndMessages(null)

                if (query.isEmpty()) {
                    SearchState.getInstance().setQuery("")
                    searchHandler.removeCallbacksAndMessages(null)
                    if (navController.currentDestination?.id == R.id.nav_search) {
                        navController.popBackStack(R.id.nav_picture, false)
                    }
                    return
                }

                searchHandler.postDelayed({
                    SearchState.getInstance().setQuery(query)
                    if (navController.currentDestination?.id != R.id.nav_search) {
                        // 搜索页只保留一份，返回时直接回到图库首页
                        navController.navigate(
                            R.id.nav_search,
                            null,
                            NavOptions.Builder()
                                .setPopUpTo(R.id.nav_picture, false)
                                .build()
                        )
                    }
                }, SEARCH_DELAY)
            }
        })
    }

    /**
     * 顶部标题栏跟随右侧页面切换
     */
    private fun setupToolbarTitle(navController: NavController) {
        if (materialToolbar == null && captionText == null) {
            return
        }
        navController.addOnDestinationChangedListener { _, destination, _ ->
            setCaptionTitle(titleOf(destination))
        }
        navController.currentDestination?.let { setCaptionTitle(titleOf(it)) }
    }

    /**
     * w800dp 布局把标题放在 toolbar_layout 的 caption_text 里（Toolbar 自带的标题
     * 会被自定义子 View 挤到屏幕外），其它布局继续使用 MaterialToolbar 自带标题。
     */
    private fun setCaptionTitle(title: CharSequence) {
        val label = captionText
        if (label != null) {
            // 标题显隐交给布局控制（caption_text 设为 gone 时即不显示标题）
            label.text = title
            materialToolbar?.title = null
        } else {
            materialToolbar?.title = title
        }
    }

    private fun titleOf(destination: NavDestination): CharSequence {
        if (destination.id == R.id.nav_search) {
            val query = SearchState.getInstance().query
            if (!query.isNullOrEmpty()) {
                return getString(R.string.search_result_title, query)
            }
        }
        return destination.label ?: getString(R.string.app_name)
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
                        decorView.width - 200,
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

    companion object {
        private const val SEARCH_DELAY = 250L
    }
}
