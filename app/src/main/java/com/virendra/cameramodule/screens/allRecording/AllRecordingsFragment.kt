package com.virendra.cameramodule.screens.allRecording

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.virendra.cameramodule.R
import com.virendra.cameramodule.constants.PageConstants
import com.virendra.cameramodule.constants.SharedPrefConstants
import com.virendra.cameramodule.databinding.FragmentAllRecordingsBinding
import com.virendra.cameramodule.screens.player.PlayerActivity
import com.virendra.cameramodule.utils.SharedPrefsManager


/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class AllRecordingsFragment : Fragment() {

    private var _binding: FragmentAllRecordingsBinding? = null
    private val binding get() = _binding!!

    private val videoSets: MutableList<String> = mutableListOf()
    private lateinit var progressDialog: ProgressBar
    private lateinit var adapter: RecordedListAdapter
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        requireActivity().window.statusBarColor = ContextCompat.getColor(requireActivity(),R.color.teal_700);

        _binding = FragmentAllRecordingsBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initUI()

        binding.fab.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        }
    }

    override fun onResume() {
        super.onResume()
        getVideoListData()
    }

    private fun initUI() {
        progressDialog = ProgressBar(requireContext(), null, android.R.attr.progressBarStyleLarge)
        val params = ConstraintLayout.LayoutParams(100, 100)
        params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
        params.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
        params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
        params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
        binding.parentView.addView(progressDialog, params)

        adapter = RecordedListAdapter(videoSets, requireContext()){ data ->
            run {
                val intent = Intent(context, PlayerActivity::class.java)
                intent.putExtra(PageConstants.VIDEO_URI, data)
                startActivity(intent)
            }
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        progressDialog.visibility = View.VISIBLE
    }

    private fun getVideoListData() {
        val set = SharedPrefsManager.newInstance(requireContext()).getStringSet(SharedPrefConstants.VIDEO_LIST_KEY)
        videoSets.clear()
        set?.forEachIndexed{ index, data ->
            run {
                val uri = Uri.parse(data)
                val resolver: ContentResolver = requireActivity().contentResolver
                val cursor = resolver.query(uri, null, null, null, null)
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        videoSets.add(data)
                    } else {
                        set.remove(data)
                    }
                    cursor.close()
                } else {
                    set.remove(data)
                }
            }
        }

        if(set?.size != videoSets.size){
            SharedPrefsManager.newInstance(requireContext()).storeVideoList(SharedPrefConstants.VIDEO_LIST_KEY, set!!)
        }

        adapter.notifyDataSetChanged()

        progressDialog.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}