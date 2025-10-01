package com.dynamite.proyectox

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember // AÑADIR IMPORT
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder // AÑADIR IMPORT
import androidx.navigation.NavController // AÑADIR IMPORT
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navigation // AÑADIR IMPORT
import com.dynamite.proyectox.presentation.cart.CartScreen
import com.dynamite.proyectox.presentation.cart.CartViewModel
import com.dynamite.proyectox.presentation.home.HomeScreen
import com.dynamite.proyectox.presentation.login.LoginScreen
import com.dynamite.proyectox.presentation.order.OrderScreen
import com.dynamite.proyectox.presentation.order.OrderViewModel
import com.dynamite.proyectox.ui.theme.ProyectoXTheme
import dagger.hilt.android.AndroidEntryPoint

// Argument Keys
const val ARG_TABLE_NUMBER = "tableNumber"

// NUEVA RUTA PARA EL GRAFO
const val ORDER_GRAPH_ROUTE = "order_flow"

// Definición de rutas
sealed class Screen(val route: String) {
    object LoginScreen : Screen("login_screen")
    object HomeScreen : Screen("home_screen")
    object OrderScreen : Screen("order_screen/{$ARG_TABLE_NUMBER}") {
        fun withArgs(tableNumber: Int): String {
            return "order_screen/$tableNumber"
        }
    }
    object CartScreen : Screen("cart_screen/{$ARG_TABLE_NUMBER}") {
        fun withArgs(tableNumber: Int): String {
            return "cart_screen/$tableNumber"
        }
    }
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ProyectoXTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.LoginScreen.route) {
        composable(route = Screen.LoginScreen.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.HomeScreen.route) {
                        popUpTo(Screen.LoginScreen.route) { inclusive = true }
                    }
                }
            )
        }
        composable(route = Screen.HomeScreen.route) {
            HomeScreen(navController = navController)
        }

        // GRAFO DE NAVEGACIÓN ANIDADO PARA EL FLUJO DEL PEDIDO
        orderFlowGraph(navController)
    }
}

// FUNCIÓN DE EXTENSIÓN PARA EL GRAFO ANIDADO
fun NavGraphBuilder.orderFlowGraph(navController: NavController) {
    navigation(
        // La navegación a OrderScreen desde HomeScreen seguirá funcionando
        startDestination = Screen.OrderScreen.route,
        route = ORDER_GRAPH_ROUTE
    ) {
        composable(
            route = Screen.OrderScreen.route,
            arguments = listOf(navArgument(ARG_TABLE_NUMBER) { type = NavType.IntType })
        ) { backStackEntry ->
            // Obtenemos el NavBackStackEntry del grafo padre
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(ORDER_GRAPH_ROUTE)
            }
            // Creamos el CartViewModel acotado al grafo padre
            val cartViewModel: CartViewModel = hiltViewModel(parentEntry)
            val orderViewModel: OrderViewModel = hiltViewModel()

            Log.d("ViewModelCheck", "OrderScreen usando CartVM: $cartViewModel")

            val orderUiState by orderViewModel.uiState.collectAsStateWithLifecycle()
            val cartUiState by cartViewModel.uiState.collectAsStateWithLifecycle()

            OrderScreen(
                navController = navController,
                state = orderUiState,
                cartItemCount = cartUiState.cartItems.sumOf { it.quantity }, // <-- CAMBIO APLICADO
                onSearchQueryChanged = orderViewModel::onSearchQueryChanged,
                onCategorySelected = orderViewModel::onCategorySelected,
                onAddProductClicked = cartViewModel::addProductToCart,
                onNavigateToCart = {
                    val currentTableNumber = orderUiState.tableNumber
                    if (currentTableNumber != null && currentTableNumber != 0) {
                        navController.navigate(Screen.CartScreen.withArgs(currentTableNumber))
                    } else {
                        Log.e("Navigation", "Número de mesa inválido para navegar al carrito")
                    }
                }
            )
        }
        composable(
            route = Screen.CartScreen.route,
            arguments = listOf(navArgument(ARG_TABLE_NUMBER) { type = NavType.IntType })
        ) { backStackEntry ->
            // Obtenemos el NavBackStackEntry del grafo padre
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(ORDER_GRAPH_ROUTE)
            }
            // Creamos el CartViewModel acotado al grafo padre (será la misma instancia)
            val cartViewModel: CartViewModel = hiltViewModel(parentEntry)
            
            Log.d("ViewModelCheck", "CartScreen usando CartVM: $cartViewModel")

            val uiState by cartViewModel.uiState.collectAsStateWithLifecycle()
            val tableNumber = backStackEntry.arguments?.getInt(ARG_TABLE_NUMBER) ?: 0

            CartScreen(
                navController = navController,
                tableNumber = tableNumber,
                state = uiState,
                onIncrementItem = cartViewModel::incrementQuantity,
                onDecrementItem = cartViewModel::decrementQuantity,
                onRemoveItem = cartViewModel::removeItem,
                onNotesChanged = cartViewModel::updateNotes,
                onSubmitOrder = {
                    Log.d("Cart", "Enviar pedido para mesa $tableNumber")
                }
            )
        }
    }
}
