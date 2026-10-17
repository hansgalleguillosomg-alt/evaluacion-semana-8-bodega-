package com.example.comercializadoraangel.data;

/** Fila de la tabla PRODUCTO (RF-03, 04, 05, 07, 08). sku_codigo es único (UK). */
public class Producto {

    public int idProducto;
    public String skuCodigo = "";
    public String nombre = "";
    public String descripcion;
    public int idCategoria;

    /** Entero >= 0 */
    public int stockActual;
    public int stockMinimo;

    /** 1 = activo, 0 = baja lógica */
    public int activo = 1;
}
