package com.example.comercializadoraangel;

import android.database.sqlite.SQLiteConstraintException;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.comercializadoraangel.data.DBHelper;
import com.example.comercializadoraangel.data.Categoria;
import com.example.comercializadoraangel.data.Producto;
import com.example.comercializadoraangel.util.SessionManager;
import com.example.comercializadoraangel.util.UiUtils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

/** RF-03 (registrar producto) y RF-07 (modificar producto). Solo para el administrador. */
public class ProductoFormActivity extends AppCompatActivity {

    public static final String EXTRA_ID_PRODUCTO = "id_producto";

    private TextInputLayout tilSku, tilNombre, tilStockActual, tilStockMinimo;
    private TextInputEditText etSku, etNombre, etDescripcion, etStockActual, etStockMinimo;
    private Spinner spCategoria;
    private TextView tvAviso;
    private ArrayAdapter<String> categoriasAdapter;
    private MaterialButton btnGuardar;

    private DBHelper dbHelper;
    private int idProducto = -1;
    private boolean editando = false;

    private final List<Categoria> categorias = new ArrayList<>();
    private Producto productoActual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        SessionManager session = new SessionManager(this);
        if (!session.haySesion() || !session.esAdministrador()) {
            finish();
            return;
        }

        setContentView(R.layout.activity_producto_form);
        UiUtils.aplicarInsets(findViewById(R.id.main));

        dbHelper = new DBHelper(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        tilSku = findViewById(R.id.tilSku);
        tilNombre = findViewById(R.id.tilNombre);
        tilStockActual = findViewById(R.id.tilStockActual);
        tilStockMinimo = findViewById(R.id.tilStockMinimo);
        etSku = findViewById(R.id.etSku);
        etNombre = findViewById(R.id.etNombre);
        etDescripcion = findViewById(R.id.etDescripcion);
        etStockActual = findViewById(R.id.etStockActual);
        etStockMinimo = findViewById(R.id.etStockMinimo);
        spCategoria = findViewById(R.id.spCategoria);
        tvAviso = findViewById(R.id.tvAviso);
        btnGuardar = findViewById(R.id.btnGuardar);

        idProducto = getIntent().getIntExtra(EXTRA_ID_PRODUCTO, -1);
        editando = idProducto != -1;

        if (editando) {
            getSupportActionBar().setTitle("Modificar producto");
            etSku.setEnabled(false);
            tvAviso.setVisibility(View.VISIBLE);
        } else {
            getSupportActionBar().setTitle("Nuevo producto");
        }

        // CU-10: categorías activas para el selector
        categorias.addAll(dbHelper.listarCategoriasActivas());
        List<String> nombres = new ArrayList<>();
        for (Categoria c : categorias) {
            nombres.add(c.nombreCategoria);
        }
        categoriasAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, nombres);
        categoriasAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCategoria.setAdapter(categoriasAdapter);

        if (editando) {
            cargarProducto();
        }

        btnGuardar.setOnClickListener(v -> guardar());
    }

    /** RF-07: carga los datos del producto a modificar y selecciona su categoría en el Spinner. */
    private void cargarProducto() {
        final Producto p = dbHelper.buscarProductoPorId(idProducto);
        if (p == null) {
            finish();
            return;
        }
        productoActual = p;
        etSku.setText(p.skuCodigo);
        etNombre.setText(p.nombre);
        etDescripcion.setText(p.descripcion);
        etStockActual.setText(String.valueOf(p.stockActual));
        etStockMinimo.setText(String.valueOf(p.stockMinimo));

        boolean encontrada = false;
        for (int i = 0; i < categorias.size(); i++) {
            if (categorias.get(i).idCategoria == p.idCategoria) {
                spCategoria.setSelection(i);
                encontrada = true;
                break;
            }
        }
        // Si la categoría del producto fue deshabilitada, igual se muestra para no cambiarla sin querer
        if (!encontrada) {
            for (Categoria c : dbHelper.listarTodasLasCategorias()) {
                if (c.idCategoria == p.idCategoria) {
                    categorias.add(c);
                    categoriasAdapter.add(c.nombreCategoria + " (deshabilitada)");
                    spCategoria.setSelection(categorias.size() - 1);
                    break;
                }
            }
        }
    }

    private void guardar() {
        tilSku.setError(null);
        tilNombre.setError(null);
        tilStockActual.setError(null);
        tilStockMinimo.setError(null);

        final String sku = UiUtils.texto(etSku);
        final String nombre = UiUtils.texto(etNombre);
        final String descripcion = UiUtils.texto(etDescripcion);
        final int stockMinimo = parsearEntero(UiUtils.texto(etStockMinimo));
        final int stockActual = parsearEntero(UiUtils.texto(etStockActual));

        boolean valido = true;
        if (!editando && sku.isEmpty()) {
            tilSku.setError("El código SKU es obligatorio");
            valido = false;
        }
        if (nombre.isEmpty()) {
            tilNombre.setError("El nombre es obligatorio");
            valido = false;
        }
        if (stockActual < 0) {
            tilStockActual.setError("Ingrese un número entero mayor o igual a 0");
            valido = false;
        }
        if (stockMinimo < 0) {
            tilStockMinimo.setError("Ingrese un número entero mayor o igual a 0");
            valido = false;
        }
        int posicion = spCategoria.getSelectedItemPosition();
        if (categorias.isEmpty() || posicion < 0 || posicion >= categorias.size()) {
            UiUtils.mensaje(findViewById(R.id.main),
                    "Primero registre una categoría para poder asignarla al producto");
            valido = false;
        }
        if (!valido) {
            return;
        }

        final int idCategoria = categorias.get(posicion).idCategoria;
        if (editando) {
            // RF-07
            dbHelper.actualizarProducto(idProducto, nombre, descripcion, stockActual, stockMinimo, idCategoria);
            Toast.makeText(this, "Producto actualizado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // RF-03: el SKU debe ser único
        if (dbHelper.contarProductosPorSku(sku) > 0) {
            skuDuplicado();
            return;
        }

        Producto p = new Producto();
        p.skuCodigo = sku;
        p.nombre = nombre;
        p.descripcion = descripcion;
        p.idCategoria = idCategoria;
        p.stockActual = stockActual;
        p.stockMinimo = stockMinimo;
        p.activo = 1;

        try {
            dbHelper.insertarProducto(p);
        } catch (SQLiteConstraintException e) {
            skuDuplicado();
            return;
        }

        Toast.makeText(this, "Producto registrado", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void skuDuplicado() {
        tilSku.setError("El código SKU ya existe");
    }

    /** Devuelve el entero (>= 0) o -1 si el texto no es válido. */
    private int parsearEntero(String texto) {
        try {
            int valor = Integer.parseInt(texto);
            return valor >= 0 ? valor : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
