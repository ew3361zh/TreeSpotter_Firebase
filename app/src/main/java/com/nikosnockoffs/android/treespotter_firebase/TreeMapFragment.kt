package com.nikosnockoffs.android.treespotter_firebase

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup


/**
 * A simple [Fragment] subclass.
 * Use the [TreeMapFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class TreeMapFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val mainView = inflater.inflate(R.layout.fragment_tree_map, container, false)


        return mainView
    }

    companion object {
        @JvmStatic
        fun newInstance() = TreeMapFragment()
    }
}