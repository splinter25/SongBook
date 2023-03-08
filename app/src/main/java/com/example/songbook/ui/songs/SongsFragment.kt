package com.example.songbook.ui.songs

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.songbook.*
import com.example.songbook.data.Song
import com.example.songbook.databinding.FragmentSongsBinding
import com.example.songbook.util.onQueryTextChanged
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SongsFragment : Fragment(), UserSongsListAdapter.OnItemClickListener {

    private var _binding: FragmentSongsBinding? = null
    private lateinit var searchView: SearchView
    private val args: SongsFragmentArgs by navArgs()

    private val viewModel : SongsViewModel by viewModels()

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSongsBinding.inflate(inflater, container, false)
        setupMenu()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val songAdapter = UserSongsListAdapter(this)

        binding.recycleViewSongs.apply {
            layoutManager = LinearLayoutManager(activity)
            adapter = songAdapter
        }

        viewModel.bandWithSongs.observe(viewLifecycleOwner) {
            songAdapter.submitList(it.songs)
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.songsEvent.collect() { event ->
                 when (event) {
                     is SongsViewModel.SongsEvent.NavigateToSingleSongScreen -> {
                         val action = SongsFragmentDirections.actionSongsFragmentToSingleSongFragment(event.song, event.song.songName)
                         findNavController().navigate(action)
                     }
                 }
            }
        }

        viewModel.onBandLoaded(args.bandWithSongs)

    }

    private fun setupMenu() {
        (requireActivity() as MenuHost).addMenuProvider(object : MenuProvider {

            override fun onPrepareMenu(menu: Menu) {
                super.onPrepareMenu(menu)
                val favoriteIcon = menu.findItem(R.id.action_add_to_favorite)
                favoriteIcon.isVisible = false
            }
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.top_app_bar, menu)

                val search = menu.findItem(R.id.action_search)
                searchView = search.actionView as SearchView

                val pendingQuery = viewModel.searchQuery.value
                if (pendingQuery.isNotEmpty()) {
                    search.expandActionView()
                    searchView.setQuery(pendingQuery, false)
                }
                searchView.onQueryTextChanged {
                    viewModel.searchQuery.value = it
                }

            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return false
            }

        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onItemClick(song: Song) {
        viewModel.onSongSelected(song)
    }

    override fun addToFavorite(song: Song, isFavorite: Boolean) {
        viewModel.addToFavorite(song, isFavorite)
    }
}