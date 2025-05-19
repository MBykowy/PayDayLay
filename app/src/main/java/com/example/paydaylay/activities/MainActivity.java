package com.example.paydaylay.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.example.paydaylay.R;
import com.example.paydaylay.firebase.AuthManager;
import com.example.paydaylay.utils.LocaleHelper;
import com.example.paydaylay.utils.NotificationUtils;
import com.google.android.material.navigation.NavigationView;

import com.example.paydaylay.fragments.DashboardFragment;
import com.example.paydaylay.fragments.TransactionsFragment;
import com.example.paydaylay.fragments.CategoriesFragment;
import com.example.paydaylay.fragments.ChartsFragment;
import com.example.paydaylay.fragments.BudgetFragment;

/**
 * Główna aktywność aplikacji, która obsługuje nawigację pomiędzy różnymi fragmentami
 * oraz zarządza szufladą nawigacyjną.
 */
public class MainActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout; // Layout szuflady nawigacyjnej
    private NavigationView navigationView; // Widok nawigacji
    private Toolbar toolbar; // Pasek narzędzi
    private AuthManager authManager; // Menedżer uwierzytelniania użytkownika

    /**
     * Dołącza kontekst bazowy z odpowiednimi ustawieniami lokalizacji.
     *
     * @param newBase bazowy kontekst aplikacji
     */
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    /**
     * Wywoływane podczas tworzenia aktywności. Inicjalizuje widoki, sprawdza stan logowania
     * użytkownika oraz ustawia domyślny fragment.
     *
     * @param savedInstanceState Jeżeli aktywność jest ponownie inicjowana po wcześniejszym zamknięciu,
     *                           ten Bundle zawiera dane, które zostały ostatnio zapisane w onSaveInstanceState(Bundle).
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Żądanie uprawnień do powiadomień
        NotificationUtils.requestNotificationPermission(this);

        authManager = new AuthManager();

        // Sprawdzenie, czy użytkownik jest zalogowany
        if (!authManager.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Inicjalizacja paska narzędzi
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Inicjalizacja szuflady nawigacyjnej
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Konfiguracja przełącznika szuflady
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Ustawienie informacji o użytkowniku w nagłówku nawigacji
        View headerView = navigationView.getHeaderView(0);
        TextView navUsername = headerView.findViewById(R.id.nav_header_username);
        if (authManager.getCurrentUser() != null) {
            navUsername.setText(authManager.getCurrentUser().getEmail());
        }

        // Ustawienie domyślnego fragmentu (DashboardFragment)
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new DashboardFragment())
                    .commit();
            navigationView.setCheckedItem(R.id.nav_dashboard);
            setTitle(R.string.menu_dashboard);
        }
    }

    /**
     * Obsługuje wybór elementów z menu nawigacyjnego.
     *
     * @param item Wybrany element menu.
     * @return Zwraca true, jeśli zdarzenie zostało obsłużone, w przeciwnym razie false.
     */
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Fragment fragment = null;
        int itemId = item.getItemId();

        // Obsługa wyboru poszczególnych elementów menu
        if (itemId == R.id.nav_dashboard) {
            fragment = new DashboardFragment();
            setTitle(R.string.menu_dashboard);
        } else if (itemId == R.id.nav_transactions) {
            fragment = new TransactionsFragment();
            setTitle(R.string.menu_transactions);
        } else if (itemId == R.id.nav_categories) {
            fragment = new CategoriesFragment();
            setTitle(R.string.menu_categories);
        } else if (itemId == R.id.nav_charts) {
            fragment = new ChartsFragment();
            setTitle(R.string.menu_charts);
        } else if (itemId == R.id.nav_budgets) {
            fragment = new BudgetFragment();
            setTitle(R.string.title_budgets);
        } else if (itemId == R.id.nav_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
        } else if (itemId == R.id.nav_logout) {
            // Wylogowanie użytkownika
            authManager.logoutUser(this);
            authManager.clearUserSession(this);
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return true;
        }

        // Zastąpienie fragmentu, jeśli został wybrany
        if (fragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
        }

        // Zamknięcie szuflady nawigacyjnej
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * Obsługuje naciśnięcie przycisku "wstecz".
     * Jeśli szuflada nawigacyjna jest otwarta, zamyka ją, w przeciwnym razie wywołuje domyślne zachowanie.
     */
    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}