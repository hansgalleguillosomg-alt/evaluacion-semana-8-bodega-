package com.example.comercializadoraangel.data;

/** Resultado del JOIN PRODUCTO + CATEGORIA para mostrar en el catálogo. */
public class ProductoItem {

    public int idProducto;
    public String skuCodigo;
    public String nombre;
    public String descripcion;
    public int stockActual;
    public int stockMinimo;
    public int activo;
    public int idCategoria;
    public String nombreCategoria;
}
