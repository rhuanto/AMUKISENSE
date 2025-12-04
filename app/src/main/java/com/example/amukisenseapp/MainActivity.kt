package com.example.amukisenseapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.rememberNavController
import com.example.amukisenseapp.navigation.AppNavigation
import com.example.amukisenseapp.navigation.Screen
import com.example.amukisenseapp.ui.theme.AMUKISENSEAppTheme
import com.example.amukisenseapp.ui.viewmodel.AuthViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var authViewModel: AuthViewModel

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            account.idToken?.let { idToken ->
                authViewModel.handleGoogleSignInResult(idToken)
            } ?: run {
                Toast.makeText(this, "Error: No se obtuvo el token", Toast.LENGTH_SHORT).show()
                authViewModel.clearError()
            }
        } catch (e: ApiException) {
            Toast.makeText(this, "Error de Google Sign-In: ${e.message}", Toast.LENGTH_SHORT).show()
            authViewModel.clearError()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Inicializar Firebase
        FirebaseApp.initializeApp(this)

        // Configurar Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
        
        // Inicializar ViewModel
        authViewModel = ViewModelProvider(this)[AuthViewModel::class.java]

        setContent {
            AMUKISENSEAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppContent(
                        authViewModel = authViewModel,
                        onGoogleSignInClick = {
                            val signInIntent = googleSignInClient.signInIntent
                            googleSignInLauncher.launch(signInIntent)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AppContent(
    authViewModel: AuthViewModel,
    onGoogleSignInClick: () -> Unit
) {
    val navController = rememberNavController()
    val authState by authViewModel.authState.collectAsState()
    
    // Observar cambios de autenticación y navegar automáticamente
    LaunchedEffect(authState) {
        when (authState) {
            is AuthViewModel.AuthState.Authenticated -> {
                // Navegar a MapaMedidas
                navController.navigate(Screen.MapaMedidas.route) {
                    popUpTo(Screen.Inicio.route) { inclusive = true }
                }
                // Resetear estado después de navegar
                authViewModel.clearError()
            }
            else -> {
                // No hacer nada en otros estados
            }
        }
    }
    
    // Determinar pantalla inicial según estado de autenticación
    val startDestination = if (FirebaseAuth.getInstance().currentUser != null) {
        Screen.MapaMedidas.route
    } else {
        Screen.Inicio.route
    }

    AppNavigation(
        navController = navController,
        startDestination = startDestination,
        onGoogleSignInClick = onGoogleSignInClick,
        authViewModel = authViewModel
    )
}