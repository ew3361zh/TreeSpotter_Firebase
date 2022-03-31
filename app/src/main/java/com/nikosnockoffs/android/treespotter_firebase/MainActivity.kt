package com.nikosnockoffs.android.treespotter_firebase

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.*

private const val TAG = "MAIN_ACTIVITY"

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val db = Firebase.firestore

        val tree = Tree("Hemlock", true, Date())
//        db.collection("trees").add(tree)

        // to add data
//        val tree = mapOf("name" to "oak", "dateSpotted" to Date())
//
//        db.collection("trees").add(tree)

        // to read data
//        db.collection("trees").get().addOnSuccessListener { treeDocuments ->
//            for (treeDoc in treeDocuments) {
//                val name = treeDoc["name"]
//                val dateSpotted = treeDoc["dateSpotted"]
//                val path = treeDoc.reference.path
//                // can try to read a field that doesn't exist and app won't crash, will return null
//                // therefore can read fields that may exist in some documents and not in others
//                Log.d(TAG, "$name, $dateSpotted $path")
//            }
//        }

        // to get updating data
//        db.collection("trees").addSnapshotListener { treeDocuments, error ->
        db.collection("trees")
            .whereEqualTo("name", "Hemlock") // filter by field values
//            .whereEqualTo("favorite", true) // can pile on additional whereEqualTos
            .orderBy("dateSpotted", Query.Direction.DESCENDING) // sorting data - will prompt setting up an index in Firebase, follow link to activate, takes a few minutes
            .limit(2) // limit number of responses
            .addSnapshotListener { treeDocuments, error ->

            if (error != null) {
                Log.d(TAG, "Error getting all trees", error)
            }
            if (treeDocuments != null) {
                for (treeDoc in treeDocuments) {
//                    val name = treeDoc["name"]
//                    val dateSpotted = treeDoc["dateSpotted"]
//                    val favorite = treeDoc["favorite"]
                    val treeFromFirebase = treeDoc.toObject(Tree::class.java)
                    val path = treeDoc.reference.path
                    // can try to read a field that doesn't exist and app won't crash, will return null
                    // therefore can read fields that may exist in some documents and not in others
//                    Log.d(TAG, "$name, $dateSpotted $favorite $path")
                    Log.d(TAG, "$treeFromFirebase $path")

                }
            }

        }
    }
}