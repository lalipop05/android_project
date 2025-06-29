package com.lali.dnd.services

import com.google.android.gms.tasks.Task
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.HttpsCallableResult
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase

object FirebaseFunctionsManager {
    private val functions = Firebase.functions

    fun getInstance(): FirebaseFunctions = functions

    fun callFunction(functionName: String,
                     data: Map<String, Any>? = null,
                     onSuccess: (Map<String, Any>?) -> Unit,
                     onFailure: (Exception) -> Unit)
    {
        functions.getHttpsCallable(functionName)
            .call(data).addOnSuccessListener { result ->
                val responseData = result.data as? Map<String, Any>
                onSuccess(responseData)

            }.addOnFailureListener(onFailure)
    }

    fun callFunction(functionName: String,
                     data: Map<String, Any>? = null): Task<HttpsCallableResult> {
        return functions.getHttpsCallable(functionName).call(data)
    }
}