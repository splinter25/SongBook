package com.example.songbook.ui.singlesong

import android.animation.ValueAnimator
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.view.View.GONE
import android.view.animation.LinearInterpolator
import android.widget.ScrollView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.navArgs
import com.example.songbook.R
import com.example.songbook.databinding.FragmentSingleSongBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class SingleSongFragment : Fragment(),
    BottomSheetChangeTextSize.BottomSheetListener {

    private val args: SingleSongFragmentArgs by navArgs()
    private val viewModel: SingleSongViewModel by viewModels()

    private var _binding: FragmentSingleSongBinding? = null
    private val binding get() = _binding!!

    private lateinit var addToFavoriteIcon: MenuItem

    private lateinit var iconScroll: MenuItem
    private lateinit var scrollView: ScrollView

    private var customAnimationSpeed = 20_000

    private var scrolledHeight: Int = 0

    private var animator: ValueAnimator? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSingleSongBinding.inflate(inflater, container, false)
        setupMenu()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupFromSharPref()

        binding.textViewTextSong.text = args.song.textSong
        scrollView = binding.scrollView

        viewModel.isFavorite = viewModel.favoriteDisposableValue.value ?: args.song.isFavorite
        viewModel.favoriteDisposableValue.observe(viewLifecycleOwner) { favValue ->
            if (favValue) {
                addToFavoriteIcon.setIcon(R.drawable.ic_favorite_checked)
                showAddCustomToast()
            } else {
                addToFavoriteIcon.setIcon(R.drawable.ic_favorite_unchecked)
                showRemoveCustomToast()
            }
        }

        val orientation = resources.configuration.orientation
        checkScreenOrientation(orientation)

        viewModel.isUserScroll.observe(viewLifecycleOwner) { isUserScrollValue ->
            if (isUserScrollValue) {
                scrolledHeight = binding.scrollView.scrollY
                startScrolling()
            } else {
                stopScrolling()
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        checkScreenOrientation(newConfig.orientation)
    }

    private fun checkScreenOrientation(orientation: Int) {
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            hideBottomNavigation()
        } else if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            showBottomNavigation()
        }
    }

    private fun hideBottomNavigation() {
        if (isAdded) {
            val activity = this.requireActivity()
            val navBar = activity.findViewById<BottomNavigationView>(R.id.bottom_nav_view)
            navBar?.visibility = GONE
        }
    }

    private fun showBottomNavigation() {
        if (isAdded) {
            val activity = this.requireActivity()
            val navBar = activity.findViewById<BottomNavigationView>(R.id.bottom_nav_view)
            navBar?.visibility = View.VISIBLE
        }
    }

    private fun setupMenu() {
        (requireActivity() as MenuHost).addMenuProvider(object : MenuProvider {

            override fun onPrepareMenu(menu: Menu) {
                super.onPrepareMenu(menu)
                val changeTextSizeIcon = menu.findItem(R.id.action_change_text_size)
                changeTextSizeIcon.isVisible = true
                val moreIcon = menu.findItem(R.id.action_more)
                moreIcon.isVisible = true

                addToFavoriteIcon = menu.findItem(R.id.action_add_to_favorite)
                addToFavoriteIcon.isVisible = true
                iconScroll = menu.findItem(R.id.action_play)
                iconScroll.isVisible = true

                if (viewModel.isFavorite) {
                    addToFavoriteIcon.setIcon(R.drawable.ic_favorite_checked)
                } else {
                    addToFavoriteIcon.setIcon(R.drawable.ic_favorite_unchecked)
                }

                observePlayIconStatus()
            }

            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.top_app_bar, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                when (menuItem.itemId) {
                    R.id.action_add_to_favorite -> {
                        viewModel.setFavorite(args.song)

                        return true
                    }

                    R.id.action_change_text_size -> {
                        val bottomSheet = BottomSheetChangeTextSize(this@SingleSongFragment)
                        bottomSheet.show(
                            parentFragmentManager,
                            BottomSheetChangeTextSize.KEY_SHOW_TEXT_SIZE
                        )
                        return true
                    }

                    R.id.action_play -> {
                        viewModel.switchPlayIcon()
                        viewModel.switchUserScrollValue()

                        return true
                    }
                }
                return false
            }

        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun observePlayIconStatus() {
        viewModel.isScrollIcon.observe(viewLifecycleOwner) { autoScroll ->
            if (autoScroll) {
                // set pause icon active when text is auto-scrolling
                iconScroll.icon = getDrawable(requireContext(), R.drawable.ic_pause)
            } else {
                // set play icon active when text don't scroll
                iconScroll.icon = getDrawable(requireContext(), R.drawable.ic_play)
            }
        }
    }

    private fun observeAndCalculateNewScrollViewPosition() {
        scrollView.setOnTouchListener { _, event ->
            if (animator != null && viewModel.isUserScroll.value == true) {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        stopScrolling()
                    }

                    MotionEvent.ACTION_UP -> {
                        if (viewModel.isUserScroll.value == true) {
                            scrolledHeight = binding.scrollView.scrollY
                            startScrolling()
                        }
                    }
                }
            }
            false
        }
    }

    private fun startScrolling() {
        val textViewHeight = binding.textViewTextSong.height
        val rootLayoutHeight = binding.rootLayout.height

        val maxScroll = textViewHeight - rootLayoutHeight

        val heightsRelation: Float = scrolledHeight.toFloat() / maxScroll.toFloat()

        val animDuration = (customAnimationSpeed * (1 - heightsRelation)).toLong()

        animator = ValueAnimator.ofInt(scrolledHeight, maxScroll).apply {
            duration = animDuration
            interpolator = LinearInterpolator()

            addUpdateListener { animation ->
                val animatedValue = animation.animatedValue as Int
                scrollView.scrollTo(scrolledHeight, animatedValue)
                scrolledHeight = animatedValue
            }

            observeAndCalculateNewScrollViewPosition()

            start()
        }
    }

    private fun stopScrolling() {
        animator?.apply {
            cancel()
            removeAllListeners()
            removeAllUpdateListeners()
        }
    }

    private fun setupFromSharPref() {
        val sharedPref =
            activity?.getSharedPreferences("app_settings", AppCompatActivity.MODE_PRIVATE)

        val savedTextSize = sharedPref?.getInt("single_song_text_size", 16) ?: 16

        binding.textViewTextSong.textSize = savedTextSize.toFloat()
    }

    private fun showAddCustomToast() {
        val toast = Toast.makeText(requireContext(), "", Toast.LENGTH_SHORT)
        val root: ViewGroup? = null
        val toastLayout = layoutInflater.inflate(R.layout.custom_toast_add_layout, root)
        toast.view = toastLayout
        toast.show()
    }

    private fun showRemoveCustomToast() {
        val toast = Toast.makeText(requireContext(), "", Toast.LENGTH_SHORT)
        val root: ViewGroup? = null
        val toastLayout = layoutInflater.inflate(R.layout.custom_toast_remove_layout, root)
        toast.view = toastLayout
        toast.show()
    }

    override fun increaseText(value: Float) {
        binding.textViewTextSong.setTextSize(TypedValue.COMPLEX_UNIT_SP, value)
    }

    override fun decreaseText(value: Float) {
        binding.textViewTextSong.setTextSize(TypedValue.COMPLEX_UNIT_SP, value)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        scrollView.removeCallbacks(null)
        animator = null
    }
}