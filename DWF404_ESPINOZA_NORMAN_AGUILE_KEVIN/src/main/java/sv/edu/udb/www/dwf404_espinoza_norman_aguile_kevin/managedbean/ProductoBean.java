package sv.edu.udb.www.dwf404_espinoza_norman_aguile_kevin.managedbean;

import jakarta.annotation.PostConstruct;
import jakarta.faces.bean.ManagedBean;
import jakarta.faces.bean.SessionScoped;
import jakarta.servlet.http.Part;
import sv.edu.udb.www.dwf404_espinoza_norman_aguile_kevin.util.DBConnection;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@ManagedBean
@SessionScoped
public class ProductoBean implements Serializable {

    private List<Producto> productos;
    private Producto producto;
    private boolean editMode;
    private Part imagen; // Campo para manejar la imagen subida

    @PostConstruct
    public void init() {
        productos = new ArrayList<>();
        producto = new Producto();
        editMode = false;
        cargarProductos();
    }

    public String getImageURL(Long productId) {
        // Ruta donde se almacenan las imágenes
        return "/images/productos/" + productId + ".jpg";
    }

    public List<Producto> getProductos() {
        return productos;
    }

    public Producto getProducto() {
        return producto;
    }

    public void setProducto(Producto producto) {
        this.producto = producto;
    }

    public boolean isEditMode() {
        return editMode;
    }

    public void setEditMode(boolean editMode) {
        this.editMode = editMode;
    }

    public Part getImagen() {
        return imagen;
    }

    public void setImagen(Part imagen) {
        this.imagen = imagen;
    }

    private void cargarProductos() {
        productos.clear(); // Limpiar la lista existente
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM productos");
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Producto producto = new Producto();
                producto.setId(rs.getLong("id"));
                producto.setNombre(rs.getString("nombre"));
                producto.setDescripcion(rs.getString("descripcion"));
                producto.setPrecio(rs.getBigDecimal("precio"));
                productos.add(producto);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String agregarProducto() {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("INSERT INTO productos (nombre, descripcion, precio) VALUES (?, ?, ?)", PreparedStatement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, producto.getNombre());
            stmt.setString(2, producto.getDescripcion());
            stmt.setBigDecimal(3, producto.getPrecio());
            stmt.executeUpdate();

            // Obtener el ID generado
            ResultSet generatedKeys = stmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                Long id = generatedKeys.getLong(1);
                if (imagen != null) {
                    try (InputStream inputStream = imagen.getInputStream()) {
                        saveImage(id, inputStream);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            cargarProductos(); // Recargar la lista de productos
            producto = new Producto(); // Limpiar el objeto producto
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String editarProducto(Long id) {
        for (Producto p : productos) {
            if (p.getId().equals(id)) {
                producto = p;
                editMode = true;
                break;
            }
        }
        return null;
    }

    public String actualizarProducto() {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("UPDATE productos SET nombre = ?, descripcion = ?, precio = ? WHERE id = ?")) {

            stmt.setString(1, producto.getNombre());
            stmt.setString(2, producto.getDescripcion());
            stmt.setBigDecimal(3, producto.getPrecio());
            stmt.setLong(4, producto.getId());
            stmt.executeUpdate();

            if (imagen != null) {
                try (InputStream inputStream = imagen.getInputStream()) {
                    saveImage(producto.getId(), inputStream);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            cargarProductos(); // Recargar la lista de productos
            producto = new Producto(); // Limpiar el objeto producto
            editMode = false;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void eliminarProducto(Long id) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM productos WHERE id = ?")) {

            stmt.setLong(1, id);
            stmt.executeUpdate();

            // Eliminar la imagen correspondiente si existe
            File file = new File("/path/to/your/images/directory/" + id + ".jpg");
            if (file.exists()) {
                file.delete();
            }

            cargarProductos(); // Recargar la lista de productos
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String cancelarEdicion() {
        producto = new Producto();
        editMode = false;
        return null;
    }

    public String guardarProducto() {
        if (editMode) {
            actualizarProducto();
        } else {
            agregarProducto();
        }
        return "crudProductos?faces-redirect=true";
    }

    private void saveImage(Long productId, InputStream inputStream) throws IOException {
        File imageFile = new File("/path/to/your/images/directory/" + productId + ".jpg");
        try (FileOutputStream outputStream = new FileOutputStream(imageFile)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
    }
}
