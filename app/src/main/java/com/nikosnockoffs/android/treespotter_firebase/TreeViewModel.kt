package com.nikosnockoffs.android.treespotter_firebase

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


private const val TAG = "TREE_VIEW_MODEL"

// better practice to create repository class similar to Travel Wishlist app but this will work and keep things simple
class TreeViewModel: ViewModel() {

    // connect to firebase

    private val db = Firebase.firestore
    // might have multiple collections in firebase, good to create reference to one we want for this app
    private val treeCollectionReference = db.collection("trees")


    // common for fragment to observe mutable live data
    val latestTrees = MutableLiveData<List<Tree>>()

    // connect to firebase, query for latest 10 trees

    private val latestTreesListener = treeCollectionReference
        .orderBy("dateSpotted", Query.Direction.DESCENDING)
        .limit(10)
        .addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error fetching latest trees", error)
            }
            else if (snapshot != null) {
//                val trees = snapshot.toObjects(Tree::class.java)
                // instead of converting the entire list of tree documents to tree objects
                // we loop over list of trees in the snapshot, make an object and tie it to its firebase reference
                // then add the tree to the list
                val trees = mutableListOf<Tree>()
                for (treeDocument in snapshot) {
                    val tree = treeDocument.toObject(Tree::class.java)
                    tree.documentReference = treeDocument.reference // how we can tell which tree is which from the db
                    trees.add(tree)
                }
                Log.d(TAG, "Trees from firebase: $trees")
                latestTrees.postValue(trees) // in background
            }
        }

    fun setIsFavorite(tree: Tree, favorite: Boolean) {
        Log.d(TAG, "Updating tree $tree to favorite $favorite")
        tree.documentReference?.update("favorite", favorite)
    }

    // would ideally add confirm/fail message for UI as well but this is just in dev stage tagging for testing/info
    fun addTree(tree: Tree) {
        treeCollectionReference.add(tree).addOnSuccessListener { treeDocumentReference ->
            Log.d(TAG, "New tree added at ${treeDocumentReference.path}")
        }
            .addOnFailureListener { error ->
                Log.e(TAG, "Error adding tree $tree", error)
            }
    }

    fun deleteTree(tree: Tree) {
        tree.documentReference?.delete()
    }
}