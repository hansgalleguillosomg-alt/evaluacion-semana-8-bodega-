package com.example.comercializadoraangel.data;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.comercializadoraangel.util.PasswordUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Base de datos SQLite de la Comercializadora Angel.
 * Crea las tablas la primera vez (onCreate) y precarga los datos iniciales (Database Seeding):
 * roles, un administrador, un operador de prueba, categorías y algunos productos.
 */
public class DBHelper extends SQLiteOpenHelper {

    // Nombre físico del archivo de la base de datos y su versión
    private static final String DB_NAME = "BD_COMERCIALIZADORA.db";
    private static final int DB_VERSION = 1;

    public DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    // Activa las claves foráneas en cada conexión (SQLite las trae desactivadas)
    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    // Se ejecuta UNA SOLA VEZ, al crear la base de datos por primera vez
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE ROL (" +
                "id_rol INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "nombre_rol TEXT NOT NULL UNIQUE, " +
                "descripcion TEXT)");

        db.execSQL("CREATE TABLE USUARIO (" +
                "id_usuario INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "username TEXT NOT NULL UNIQUE, " +
                "password_hash TEXT NOT NULL, " +
                "nombre_completo TEXT NOT NULL, " +
                "id_rol INTEGER NOT NULL, " +
                "activo INTEGER NOT NULL DEFAULT 1, " +
                "FOREIGN KEY (id_rol) REFERENCES ROL(id_rol) ON DELETE RESTRICT)");

        db.execSQL("CREATE TABLE CATEGORIA (" +
                "id_categoria INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "nombre_categoria TEXT NOT NULL UNIQUE, " +
                "descripcion TEXT, " +
                "activo INTEGER NOT NULL DEFAULT 1)");

        db.execSQL("CREATE TABLE PROVEEDOR (" +
                "id_proveedor INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "rut_empresa TEXT NOT NULL UNIQUE, " +
                "razon_social TEXT NOT NULL, " +
                "telefono_contacto TEXT NOT NULL)");

        db.execSQL("CREATE TABLE PRODUCTO (" +
                "id_producto INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "sku_codigo TEXT NOT NULL UNIQUE, " +
                "nombre TEXT NOT NULL, " +
                "descripcion TEXT, " +
                "id_categoria INTEGER NOT NULL, " +
                "stock_actual INTEGER NOT NULL CHECK (stock_actual >= 0), " +
                "stock_minimo INTEGER NOT NULL DEFAULT 0, " +
                "activo INTEGER NOT NULL DEFAULT 1, " +
                "FOREIGN KEY (id_categoria) REFERENCES CATEGORIA(id_categoria) ON DELETE RESTRICT)");

        cargarDatosIniciales(db);
    }

    // Se ejecuta cuando cambia DB_VERSION: borra las tablas y las vuelve a crear
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS PRODUCTO");
        db.execSQL("DROP TABLE IF EXISTS PROVEEDOR");
        db.execSQL("DROP TABLE IF EXISTS CATEGORIA");
        db.execSQL("DROP TABLE IF EXISTS USUARIO");
        db.execSQL("DROP TABLE IF EXISTS ROL");
        onCreate(db);
    }

    private void cargarDatosIniciales(SQLiteDatabase db) {
        // Roles (id 1 = Administrador, id 2 = Operador)
        db.execSQL("INSERT INTO ROL (nombre_rol, descripcion) VALUES (?, ?)",
                new Object[]{Rol.ADMINISTRADOR, "Gestión completa: usuarios, roles, categorías, proveedores y productos"});
        db.execSQL("INSERT INTO ROL (nombre_rol, descripcion) VALUES (?, ?)",
                new Object[]{Rol.OPERADOR, "Consulta de productos y categorías"});

        // Usuarios iniciales
        db.execSQL("INSERT INTO USUARIO (username, password_hash, nombre_completo, id_rol, activo) VALUES (?, ?, ?, ?, ?)",
                new Object[]{"admin", PasswordUtils.hash("admin123"), "Nathalie Angel Rivera", 1, 1});
        db.execSQL("INSERT INTO USUARIO (username, password_hash, nombre_completo, id_rol, activo) VALUES (?, ?, ?, ?, ?)",
                new Object[]{"bodeguero", PasswordUtils.hash("bodega123"), "Bodeguero de Prueba", 2, 1});

        // Categorías del negocio
        db.execSQL("INSERT INTO CATEGORIA (nombre_categoria, descripcion, activo) VALUES (?, ?, ?)",
                new Object[]{"Frutos secos", "Nueces, almendras, maní, pistachos, etc.", 1});
        db.execSQL("INSERT INTO CATEGORIA (nombre_categoria, descripcion, activo) VALUES (?, ?, ?)",
                new Object[]{"Frutas deshidratadas", "Pasas, orejones, ciruelas secas, etc.", 1});
        db.execSQL("INSERT INTO CATEGORIA (nombre_categoria, descripcion, activo) VALUES (?, ?, ?)",
                new Object[]{"Quesos", "Quesos frescos y maduros", 1});

        // Productos de ejemplo
        insertarProductoInicial(db, "FS-001", "Nueces mariposa", "Nuez pelada, bolsa de 1 kg", 1, 40, 10);
        insertarProductoInicial(db, "FS-002", "Almendras", "Almendra natural, bolsa de 1 kg", 1, 25, 8);
        insertarProductoInicial(db, "FD-001", "Pasas rubias", "Pasas sin pepa, bolsa de 500 g", 2, 30, 10);
        insertarProductoInicial(db, "QS-001", "Queso gauda", "Gauda laminado, pieza", 3, 12, 5);
        insertarProductoInicial(db, "QS-002", "Queso mantecoso", "Queso mantecoso, pieza", 3, 15, 5);
    }

    private void insertarProductoInicial(SQLiteDatabase db, String sku, String nombre,
                                         String descripcion, int idCategoria,
                                         int stockActual, int stockMinimo) {
        db.execSQL("INSERT INTO PRODUCTO (sku_codigo, nombre, descripcion, id_categoria, stock_actual, stock_minimo, activo) "
                        + "VALUES (?, ?, ?, ?, ?, ?, 1)",
                new Object[]{sku, nombre, descripcion, idCategoria, stockActual, stockMinimo});
    }

    // =====================================================================
    //  Utilidades de lectura / escritura (cada método abre y cierra la BD)
    // =====================================================================

    /** Cuenta las filas que devuelve un SELECT COUNT(*) con parámetros. */
    private int contar(String sql, String... args) {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.rawQuery(sql, args)) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        } finally {
            db.close();
        }
    }

    private static String texto(Cursor c, String columna) {
        return c.getString(c.getColumnIndexOrThrow(columna));
    }

    private static int entero(Cursor c, String columna) {
        return c.getInt(c.getColumnIndexOrThrow(columna));
    }

    // ---------------------------- ROL ----------------------------

    public void insertarRol(Rol rol) {
        SQLiteDatabase db = getWritableDatabase();
        try {
            db.execSQL("INSERT INTO ROL (nombre_rol, descripcion) VALUES (?, ?)",
                    new Object[]{rol.nombreRol, rol.descripcion});
        } finally {
            db.close();
        }
    }

    public int contarRolesPorNombre(String nombre) {
        return contar("SELECT COUNT(*) FROM ROL WHERE nombre_rol = ? COLLATE NOCASE", nombre);
    }

    public Rol buscarRolPorId(int idRol) {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT * FROM ROL WHERE id_rol = ? LIMIT 1",
                new String[]{String.valueOf(idRol)})) {
            return c.moveToFirst() ? leerRol(c) : null;
        } finally {
            db.close();
        }
    }

    public List<Rol> listarRoles() {
        List<Rol> lista = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT * FROM ROL ORDER BY nombre_rol ASC", null)) {
            while (c.moveToNext()) {
                lista.add(leerRol(c));
            }
        } finally {
            db.close();
        }
        return lista;
    }

    private Rol leerRol(Cursor c) {
        Rol r = new Rol();
        r.idRol = entero(c, "id_rol");
        r.nombreRol = texto(c, "nombre_rol");
        r.descripcion = texto(c, "descripcion");
        return r;
    }

    // --------------------------- USUARIO ---------------------------

    /** CU-01 paso 4: consulta la entidad USUARIO por username. */
    public Usuario buscarUsuarioPorUsername(String username) {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT * FROM USUARIO WHERE username = ? COLLATE NOCASE LIMIT 1",
                new String[]{username})) {
            if (!c.moveToFirst()) {
                return null;
            }
            Usuario u = new Usuario();
            u.idUsuario = entero(c, "id_usuario");
            u.username = texto(c, "username");
            u.passwordHash = texto(c, "password_hash");
            u.nombreCompleto = texto(c, "nombre_completo");
            u.idRol = entero(c, "id_rol");
            u.activo = entero(c, "activo");
            return u;
        } finally {
            db.close();
        }
    }

    /** CU-27 flujo alternativo 6a: validación previa de username duplicado. */
    public int contarUsuariosPorUsername(String username) {
        return contar("SELECT COUNT(*) FROM USUARIO WHERE username = ? COLLATE NOCASE", username);
    }

    public void insertarUsuario(Usuario u) {
        SQLiteDatabase db = getWritableDatabase();
        try {
            db.execSQL("INSERT INTO USUARIO (username, password_hash, nombre_completo, id_rol, activo) "
                            + "VALUES (?, ?, ?, ?, ?)",
                    new Object[]{u.username, u.passwordHash, u.nombreCompleto, u.idRol, u.activo});
        } finally {
            db.close();
        }
    }

    /** RF-27: activa (1) o desactiva (0) un usuario. Un usuario inactivo no puede iniciar sesión. */
    public void cambiarEstadoUsuario(int idUsuario, int activo) {
        SQLiteDatabase db = getWritableDatabase();
        try {
            db.execSQL("UPDATE USUARIO SET activo = ? WHERE id_usuario = ?",
                    new Object[]{activo, idUsuario});
        } finally {
            db.close();
        }
    }

    /** CU-27 paso 2: listado de usuarios con el nombre de su rol. */
    public List<UsuarioItem> listarUsuariosConRol() {
        List<UsuarioItem> lista = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT u.id_usuario, u.username, u.nombre_completo, u.activo, r.nombre_rol "
                + "FROM USUARIO u INNER JOIN ROL r ON r.id_rol = u.id_rol "
                + "ORDER BY u.nombre_completo COLLATE NOCASE ASC", null)) {
            while (c.moveToNext()) {
                UsuarioItem u = new UsuarioItem();
                u.idUsuario = entero(c, "id_usuario");
                u.username = texto(c, "username");
                u.nombreCompleto = texto(c, "nombre_completo");
                u.activo = entero(c, "activo");
                u.nombreRol = texto(c, "nombre_rol");
                lista.add(u);
            }
        } finally {
            db.close();
        }
        return lista;
    }

    // -------------------------- CATEGORIA --------------------------

    /** CU-09 paso 5: valida nombre repetido (sin distinguir mayúsculas). */
    public int contarCategoriasPorNombre(String nombre) {
        return contar("SELECT COUNT(*) FROM CATEGORIA WHERE nombre_categoria = ? COLLATE NOCASE", nombre);
    }

    /** CU-09 paso 6: inserta con activo = 1 por defecto. */
    public void insertarCategoria(Categoria cat) {
        SQLiteDatabase db = getWritableDatabase();
        try {
            db.execSQL("INSERT INTO CATEGORIA (nombre_categoria, descripcion, activo) VALUES (?, ?, ?)",
                    new Object[]{cat.nombreCategoria, cat.descripcion, cat.activo});
        } finally {
            db.close();
        }
    }

    /** Todas las categorías (activas primero), para que el administrador pueda habilitarlas o deshabilitarlas. */
    public List<Categoria> listarTodasLasCategorias() {
        List<Categoria> lista = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT * FROM CATEGORIA "
                + "ORDER BY activo DESC, nombre_categoria ASC", null)) {
            while (c.moveToNext()) {
                Categoria cat = new Categoria();
                cat.idCategoria = entero(c, "id_categoria");
                cat.nombreCategoria = texto(c, "nombre_categoria");
                cat.descripcion = texto(c, "descripcion");
                cat.activo = entero(c, "activo");
                lista.add(cat);
            }
        } finally {
            db.close();
        }
        return lista;
    }

    /** Habilita (1) o deshabilita (0) una categoría. Una categoría deshabilitada no aparece al registrar productos. */
    public void cambiarEstadoCategoria(int idCategoria, int activo) {
        SQLiteDatabase db = getWritableDatabase();
        try {
            db.execSQL("UPDATE CATEGORIA SET activo = ? WHERE id_categoria = ?",
                    new Object[]{activo, idCategoria});
        } finally {
            db.close();
        }
    }

    /** CU-10 paso 2: solo categorías activas, ordenadas por nombre. */
    public List<Categoria> listarCategoriasActivas() {
        List<Categoria> lista = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT * FROM CATEGORIA WHERE activo = 1 "
                + "ORDER BY nombre_categoria ASC", null)) {
            while (c.moveToNext()) {
                Categoria cat = new Categoria();
                cat.idCategoria = entero(c, "id_categoria");
                cat.nombreCategoria = texto(c, "nombre_categoria");
                cat.descripcion = texto(c, "descripcion");
                cat.activo = entero(c, "activo");
                lista.add(cat);
            }
        } finally {
            db.close();
        }
        return lista;
    }

    // -------------------------- PROVEEDOR --------------------------

    public int contarProveedoresPorRut(String rut) {
        return contar("SELECT COUNT(*) FROM PROVEEDOR WHERE rut_empresa = ? COLLATE NOCASE", rut);
    }

    public void insertarProveedor(Proveedor p) {
        SQLiteDatabase db = getWritableDatabase();
        try {
            db.execSQL("INSERT INTO PROVEEDOR (rut_empresa, razon_social, telefono_contacto) VALUES (?, ?, ?)",
                    new Object[]{p.rutEmpresa, p.razonSocial, p.telefonoContacto});
        } finally {
            db.close();
        }
    }

    public List<Proveedor> listarProveedores() {
        List<Proveedor> lista = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT * FROM PROVEEDOR ORDER BY razon_social COLLATE NOCASE ASC", null)) {
            while (c.moveToNext()) {
                Proveedor p = new Proveedor();
                p.idProveedor = entero(c, "id_proveedor");
                p.rutEmpresa = texto(c, "rut_empresa");
                p.razonSocial = texto(c, "razon_social");
                p.telefonoContacto = texto(c, "telefono_contacto");
                lista.add(p);
            }
        } finally {
            db.close();
        }
        return lista;
    }

    // --------------------------- PRODUCTO ---------------------------

    public int contarProductosPorSku(String sku) {
        return contar("SELECT COUNT(*) FROM PRODUCTO WHERE sku_codigo = ? COLLATE NOCASE", sku);
    }

    /** RF-03: alta de producto. */
    public void insertarProducto(Producto p) {
        SQLiteDatabase db = getWritableDatabase();
        try {
            db.execSQL("INSERT INTO PRODUCTO (sku_codigo, nombre, descripcion, id_categoria, "
                            + "stock_actual, stock_minimo, activo) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    new Object[]{p.skuCodigo, p.nombre, p.descripcion, p.idCategoria,
                            p.stockActual, p.stockMinimo, p.activo});
        } finally {
            db.close();
        }
    }

    /** RF-04 y RF-05: catálogo + búsqueda por nombre o SKU (solo productos activos). */
    public List<ProductoItem> buscarProductos(String busqueda) {
        List<ProductoItem> lista = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT p.id_producto, p.sku_codigo, p.nombre, p.descripcion, "
                        + "p.stock_actual, p.stock_minimo, p.activo, p.id_categoria, c.nombre_categoria "
                        + "FROM PRODUCTO p INNER JOIN CATEGORIA c ON c.id_categoria = p.id_categoria "
                        + "WHERE p.activo = 1 "
                        + "AND (p.nombre LIKE '%' || ? || '%' OR p.sku_codigo LIKE '%' || ? || '%') "
                        + "ORDER BY p.nombre COLLATE NOCASE ASC",
                new String[]{busqueda, busqueda})) {
            while (c.moveToNext()) {
                ProductoItem p = new ProductoItem();
                p.idProducto = entero(c, "id_producto");
                p.skuCodigo = texto(c, "sku_codigo");
                p.nombre = texto(c, "nombre");
                p.descripcion = texto(c, "descripcion");
                p.stockActual = entero(c, "stock_actual");
                p.stockMinimo = entero(c, "stock_minimo");
                p.activo = entero(c, "activo");
                p.idCategoria = entero(c, "id_categoria");
                p.nombreCategoria = texto(c, "nombre_categoria");
                lista.add(p);
            }
        } finally {
            db.close();
        }
        return lista;
    }

    public Producto buscarProductoPorId(int idProducto) {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT * FROM PRODUCTO WHERE id_producto = ? LIMIT 1",
                new String[]{String.valueOf(idProducto)})) {
            if (!c.moveToFirst()) {
                return null;
            }
            Producto p = new Producto();
            p.idProducto = entero(c, "id_producto");
            p.skuCodigo = texto(c, "sku_codigo");
            p.nombre = texto(c, "nombre");
            p.descripcion = texto(c, "descripcion");
            p.idCategoria = entero(c, "id_categoria");
            p.stockActual = entero(c, "stock_actual");
            p.stockMinimo = entero(c, "stock_minimo");
            p.activo = entero(c, "activo");
            return p;
        } finally {
            db.close();
        }
    }

    /** RF-07: solo nombre, descripción, stock mínimo y categoría. */
    public void actualizarProducto(int idProducto, String nombre, String descripcion,
                                   int stockActual, int stockMinimo, int idCategoria) {
        SQLiteDatabase db = getWritableDatabase();
        try {
            db.execSQL("UPDATE PRODUCTO SET nombre = ?, descripcion = ?, stock_actual = ?, stock_minimo = ?, "
                            + "id_categoria = ? WHERE id_producto = ?",
                    new Object[]{nombre, descripcion, stockActual, stockMinimo, idCategoria, idProducto});
        } finally {
            db.close();
        }
    }

    /** RF-08: baja lógica (activo = 0). */
    public void deshabilitarProducto(int idProducto) {
        SQLiteDatabase db = getWritableDatabase();
        try {
            db.execSQL("UPDATE PRODUCTO SET activo = 0 WHERE id_producto = ?",
                    new Object[]{idProducto});
        } finally {
            db.close();
        }
    }
}
