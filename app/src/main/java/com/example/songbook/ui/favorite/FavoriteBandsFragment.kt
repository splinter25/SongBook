package com.example.songbook.ui.favorite

import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.songbook.R
import com.example.songbook.data.relations.BandWithSongs
import com.example.songbook.databinding.FragmentFavoriteBinding
import com.example.songbook.util.onQueryTextChanged
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FavoriteBandsFragment : Fragment(), FavoriteListAdapter.OnItemClickListener {

    private var _binding: FragmentFavoriteBinding? = null
    private val viewModel: FavoriteBandsViewModel by viewModels()
    private lateinit var searchView: SearchView

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoriteBinding.inflate(inflater, container, false)
        setupMenu()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val favBandsAdapter = FavoriteListAdapter(this)
        binding.recycleViewFavBands.apply {
            adapter = favBandsAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        viewModel.favBands.observe(viewLifecycleOwner) {
            favBandsAdapter.submitList(it)
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.favBandsEvent.collect() { event ->
                when (event) {
                    is FavoriteBandsViewModel.FavEvent.NavigateToFavSongsScreen -> {
                       val action = FavoriteBandsFragmentDirections
                           .actionNavigationFavoriteToFavoriteSongsFragment(event.bandWithSongs,
                               event.bandWithSongs.band.bandName)
                        findNavController().navigate(action)
                    }
                }
            }
        }
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
                    searchView.setQuery(pendingQuery,false)
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

    override fun onItemClick(bandWithSongs: BandWithSongs) {
        viewModel.onBandSelected(bandWithSongs)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}