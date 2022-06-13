package jose.daniel.somarribas

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.ListView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), CoroutineScope by MainScope(){

    val mainHandler = Handler(Looper.getMainLooper())
    var lista_archivos: MutableList<String>? = null
    var arrayAdapter: ArrayAdapter<*>? = null

    companion object{
        private const val REQUEST_SIGN_IN = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestSignIn()


        lista_archivos = mutableListOf()
        val view = findViewById<ListView>(R.id.GoogleDrive_list)
        arrayAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, lista_archivos!!)
        view.adapter = arrayAdapter
    }


    private fun requestSignIn(){
        val client = buildGoogleSignInClient()
        startActivityForResult(client.signInIntent, REQUEST_SIGN_IN)

    }

    private fun buildGoogleSignInClient(): GoogleSignInClient {
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestScopes(Scope(DriveScopes.DRIVE))
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(this, signInOptions)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.e("mensage","onActivityResult=$requestCode")
        when (requestCode) {
            REQUEST_SIGN_IN -> {
                if (resultCode == RESULT_OK && data != null) {
                    handleSignInResult(data)
                } else {
                    Log.e("ERROR","Fallo en la petición de autenticación")
                }
            }
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun handleSignInResult(result: Intent) {
        GoogleSignIn.getSignedInAccountFromIntent(result)
            .addOnSuccessListener { googleAccount ->
                // Use the authenticated account to sign in to the Drive service.
                val credential = GoogleAccountCredential.usingOAuth2(
                    this, listOf(DriveScopes.DRIVE, DriveScopes.DRIVE_FILE)
                )
                credential.selectedAccount = googleAccount.account
                val googleDriveService = Drive.Builder(
                    AndroidHttp.newCompatibleTransport(),
                    JacksonFactory.getDefaultInstance(),
                    credential
                )
                    .setApplicationName(getString(R.string.app_name))
                    .build()

                // https://developers.google.com/drive/api/v3/search-files
                // https://developers.google.com/drive/api/v3/search-parameters
                // https://developers.google.com/drive/api/v3/mime-types

                launch(Dispatchers.Default) {
                var pageToken: String? = null

                do {
                    val result = googleDriveService.files().list().apply {
                        //q = "mimeType='application/vnd.google-apps.spreadsheet'"
                        spaces = "drive"
                        fields = "nextPageToken, files(id, name)"
                        this.pageToken = pageToken

                    }.execute()
                    for (file in result.files) {
                       Log.e("Archivo","name=${file.name}, id=${file.id}")

                        mainHandler.post{
                            lista_archivos?.add(file.name)
                            arrayAdapter?.notifyDataSetChanged()
                        }

                    }
                } while (pageToken != null)
            }
    }
    .addOnFailureListener { e ->
        Log.e(e.stackTraceToString(), "Signin error")
    }
}

}