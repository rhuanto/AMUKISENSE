package com.example.amukisenseapp.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.amukisenseapp.R
import com.example.amukisenseapp.data.repository.RegistroRepository
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * VISTA 18: Estad. - Config. - Cuenta
 * 
 * Vista de gestión de información personal y métodos de acceso:
 * - Datos de cuenta (correo, idioma)
 * - Métodos de inicio de sesión (Google, Correo/Contraseña)
 * - Opción de eliminar cuenta
 * 
 * REFACTORIZADO: Ahora usa RegistroRepository en lugar de acceso directo a Firestore
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CuentaScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCorreo: () -> Unit,
    onNavigateToEliminar: () -> Unit,
    onNavigateToLinkGoogle: () -> Unit = {}
) {
    val auth = FirebaseAuth.getInstance()
    val repository = remember { RegistroRepository() }
    val currentUser = auth.currentUser
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var userName by remember { mutableStateOf("User Name") }
    var userEmail by remember { mutableStateOf("correo@gmail.com") }
    var joinDate by remember { mutableStateOf("Sep 2025") }
    var userId by remember { mutableStateOf("#10") }
    var googleLinked by remember { mutableStateOf(false) }
    var emailLinked by remember { mutableStateOf(false) }
    var isLinking by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Configurar Google Sign-In
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("811260524046-gfbn3rk8njqjc4tsmjdvopkn11u4p52c.apps.googleusercontent.com")
            .requestEmail()
            .build()
    }
    
    val googleSignInClient = remember {
        GoogleSignIn.getClient(context, gso)
    }
    
    // Launcher para Google Sign-In
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        android.util.Log.d("CuentaScreen", "Google Sign-In result code: ${result.resultCode}")
        
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                android.util.Log.d("CuentaScreen", "Google account obtenido: ${account.email}")
                android.util.Log.d("CuentaScreen", "ID Token: ${account.idToken?.take(20)}...")
                
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                
                // Vincular cuenta de Google
                currentUser?.linkWithCredential(credential)
                    ?.addOnSuccessListener {
                        android.util.Log.d("CuentaScreen", "✅ Vinculación exitosa!")
                        isLinking = false
                        googleLinked = true
                        showSuccessDialog = true
                        errorMessage = null
                    }
                    ?.addOnFailureListener { e ->
                        android.util.Log.e("CuentaScreen", "❌ Error al vincular: ${e.message}", e)
                        isLinking = false
                        errorMessage = when {
                            e.message?.contains("already in use") == true ->
                                "Esta cuenta de Google ya está registrada con otro usuario"
                            e.message?.contains("credential-already-in-use") == true ->
                                "Esta cuenta de Google ya está registrada con otro usuario"
                            e.message?.contains("provider-already-linked") == true ->
                                "Ya has vinculado una cuenta de Google previamente"
                            else -> "Error al vincular: ${e.localizedMessage}"
                        }
                    }
            } catch (e: ApiException) {
                android.util.Log.e("CuentaScreen", "❌ ApiException al obtener cuenta: ${e.statusCode} - ${e.message}", e)
                isLinking = false
                errorMessage = when (e.statusCode) {
                    // Error 12501: Usuario canceló
                    12501 -> null
                    // Error 12500: Error interno de Google Sign-In (puede ser cuenta ya en uso)
                    12500 -> "Esta cuenta de Google ya está registrada con otro usuario"
                    else -> "Error al obtener credenciales de Google (código: ${e.statusCode})"
                }
            }
        } else if (result.resultCode == Activity.RESULT_CANCELED) {
            android.util.Log.d("CuentaScreen", "⚠️ Sign-In cancelado. Posible cuenta ya en uso.")
            // Verificar si hay datos en el intent que indiquen el error específico
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                task.getResult(ApiException::class.java)
                // Si llegamos aquí sin excepción, el usuario simplemente canceló
                isLinking = false
                errorMessage = null
            } catch (e: ApiException) {
                android.util.Log.e("CuentaScreen", "❌ ApiException en RESULT_CANCELED: ${e.statusCode}", e)
                isLinking = false
                errorMessage = when (e.statusCode) {
                    12501 -> null // Usuario canceló voluntariamente
                    12500 -> "Esta cuenta de Google ya está registrada con otro usuario"
                    else -> "Esta cuenta de Google ya está registrada con otro usuario"
                }
            }
        } else {
            android.util.Log.d("CuentaScreen", "Resultado inesperado. Result code: ${result.resultCode}")
            isLinking = false
        }
    }
    
    // Obtener datos del usuario
    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            android.util.Log.d("CuentaScreen", "LaunchedEffect ejecutado para usuario: ${user.uid}")
            userEmail = user.email ?: "correo@gmail.com"
            
            // Determinar métodos vinculados
            user.providerData.forEach { profile ->
                when (profile.providerId) {
                    "google.com" -> googleLinked = true
                    "password" -> emailLinked = true
                }
            }
            
            // Obtener datos del usuario usando Repository
            scope.launch {
                repository.getUsuarioInfo(user.uid).fold(
                    onSuccess = { usuarioInfo ->
                        usuarioInfo?.let {
                            userName = it.nombreUsuario
                            android.util.Log.d("CuentaScreen", "Alias obtenido: ${it.nombreUsuario}")
                            
                            // Formatear fecha
                            it.fechaUnion?.toDate()?.let { fecha ->
                                val format = SimpleDateFormat("MMM yyyy", Locale("es", "ES"))
                                joinDate = format.format(fecha)
                                android.util.Log.d("CuentaScreen", "Fecha obtenida: $joinDate")
                            } ?: run {
                                user.metadata?.creationTimestamp?.let { timestamp ->
                                    val date = Date(timestamp)
                                    val format = SimpleDateFormat("MMM yyyy", Locale("es", "ES"))
                                    joinDate = format.format(date)
                                }
                            }
                        }
                    },
                    onFailure = { e ->
                        android.util.Log.e("CuentaScreen", "Error obteniendo datos: ${e.message}", e)
                    }
                )
                
                // Obtener usuario completo para número de usuario
                repository.getUsuario(user.uid).fold(
                    onSuccess = { usuario ->
                        usuario?.let {
                            val numeroUsuario = it.numero_usuario
                            userId = if (numeroUsuario != null) "#$numeroUsuario" else "#0"
                            android.util.Log.d("CuentaScreen", "Número usuario obtenido: $userId")
                        }
                    },
                    onFailure = { e ->
                        android.util.Log.e("CuentaScreen", "Error obteniendo número usuario", e)
                    }
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // Imagen de perfil usando ic_usuario_generico
            val userIcon = remember {
                BitmapPainter(
                    context.resources.getDrawable(R.mipmap.ic_usuario_generico, null)
                        .toBitmap()
                        .asImageBitmap()
                )
            }
            
            Image(
                painter = userIcon,
                contentDescription = "Perfil",
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Nombre de usuario
            Text(
                text = userName,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            
            // Fecha de unión
            Text(
                text = "Unido: $joinDate",
                fontSize = 14.sp,
                color = Color.Gray
            )
            
            // ID de usuario
            Text(
                text = userId,
                fontSize = 12.sp,
                color = Color.Gray
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Datos de la cuenta
            InfoField(
                label = "Correo",
                value = userEmail
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            InfoField(
                label = "Lenguaje",
                value = "Español"
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Método de logeo
            Text(
                text = "Método de logeo",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column {
                    // Google
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !googleLinked && !isLinking) { 
                                android.util.Log.d("CuentaScreen", "🔵 Click en vincular Google")
                                android.util.Log.d("CuentaScreen", "Usuario actual: ${currentUser?.email}")
                                android.util.Log.d("CuentaScreen", "Google ya vinculado: $googleLinked")
                                isLinking = true
                                errorMessage = null
                                googleSignInLauncher.launch(googleSignInClient.signInIntent)
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = if (googleLinked) Color.Gray else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Google",
                            fontSize = 16.sp,
                            modifier = Modifier.weight(1f),
                            color = if (googleLinked) Color.Gray else MaterialTheme.colorScheme.onSurface
                        )
                        
                        if (isLinking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = if (googleLinked) "Vinculado" else "Agregar",
                                fontSize = 14.sp,
                                color = if (googleLinked) Color.Gray else Color(0xFF2900CC)
                            )
                            if (!googleLinked) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = Color.Gray
                                )
                            }
                        }
                    }
                    
                    Divider()
                    
                    // Correo y Contraseña
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onNavigateToCorreo)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Email,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Correo y Contraseña",
                            fontSize = 16.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = if (emailLinked) "Vinculado" else "Agregar",
                            fontSize = 14.sp,
                            color = if (emailLinked) Color.Gray else Color(0xFF2900CC)
                        )
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = Color.Gray
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Botón Eliminar cuenta
            Button(
                onClick = onNavigateToEliminar,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White
                ),
                shape = RoundedCornerShape(28.dp),
                elevation = ButtonDefaults.buttonElevation(2.dp)
            ) {
                Text(
                    "Eliminar cuenta",
                    color = Color.Black,
                    fontSize = 16.sp
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
        
        // Mostrar mensaje de error si existe
        errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
    
    // Diálogo de éxito
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            icon = {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = "¡Vinculación exitosa!",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("Tu cuenta de Google ha sido vinculada. Ahora puedes iniciar sesión con cualquiera de los dos métodos.")
            },
            confirmButton = {
                TextButton(
                    onClick = { showSuccessDialog = false }
                ) {
                    Text("Aceptar")
                }
            }
        )
    }
}

@Composable
private fun InfoField(
    label: String,
    value: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF5F5F5)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                fontSize = 16.sp,
                color = Color.Gray
            )
        }
    }
}
