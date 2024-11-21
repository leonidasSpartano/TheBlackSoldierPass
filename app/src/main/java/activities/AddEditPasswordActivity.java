package activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.appmanagerpassword.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import models.UserPass;

public class AddEditPasswordActivity extends AppCompatActivity {

    private EditText sitioEditText, usuarioEditText, contraseñaEditText, apuntesEditText;
    private MaterialButton backButton, savePasswordButton;
    private DatabaseReference myRef;

    private String currentKey = null;  // Para almacenar el key si ya existe un registro

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_password);

        // Inicializar las vistas
        sitioEditText = findViewById(R.id.sitioEditText);
        usuarioEditText = findViewById(R.id.usuarioEditText);
        contraseñaEditText = findViewById(R.id.contraseñaEditText);
        apuntesEditText = findViewById(R.id.apuntesEditText);
        backButton = findViewById(R.id.backButton);
        savePasswordButton = findViewById(R.id.savePasswordButton);

        // Inicializar Firebase
        myRef = FirebaseDatabase.getInstance().getReference("pass");

        // Obtener los datos del intent
        Intent intent = getIntent();
        String sitio = intent.getStringExtra("nombrePagina");
        String usuario = intent.getStringExtra("nombreUsuario");
        String contraseña = intent.getStringExtra("password");
        String apuntes = intent.getStringExtra("apuntes");
        currentKey = intent.getStringExtra("key");  // Recibe el key si está editando un registro

        // Establecer los valores en los campos
        sitioEditText.setText(sitio);
        usuarioEditText.setText(usuario);
        contraseñaEditText.setText(decryptPassword(contraseña));  // Desencriptamos la contraseña
        apuntesEditText.setText(apuntes);

        // Configurar el botón Volver
        backButton.setOnClickListener(v -> onBackPressed());

        // Configurar el botón Guardar
        savePasswordButton.setOnClickListener(v -> {
            // Obtener los datos del formulario
            String sitioGuardado = sitioEditText.getText().toString();
            String usuarioGuardado = usuarioEditText.getText().toString();
            String contraseñaGuardada = contraseñaEditText.getText().toString();
            String apuntesGuardados = apuntesEditText.getText().toString();

            // Validar los campos (esto es opcional)
            if (sitioGuardado.isEmpty() || usuarioGuardado.isEmpty() || contraseñaGuardada.isEmpty()) {
                Toast.makeText(this, "Por favor complete todos los campos", Toast.LENGTH_SHORT).show();
            } else {
                savePassword(sitioGuardado, usuarioGuardado, contraseñaGuardada, apuntesGuardados);
            }
        });
    }

    // Este es el método principal que guarda o actualiza la contraseña
    private void savePassword(String sitio, String usuario, String contraseña, String apuntes) {
        // Verificar si los campos esenciales están vacíos
        if (sitio.isEmpty() || usuario.isEmpty() || contraseña.isEmpty()) {
            Toast.makeText(this, "Por favor, completa todos los campos obligatorios", Toast.LENGTH_SHORT).show();
            return;
        }

        // Obtener el ID del usuario autenticado
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        if (userId.isEmpty()) {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show();
            return;
        }

        // Obtener el ID de la tarjeta desde el Intent
        String idTarjeta = getIntent().getStringExtra("idTarjeta");
        if (idTarjeta == null || idTarjeta.isEmpty()) {
            idTarjeta = FirebaseDatabase.getInstance().getReference().push().getKey();  // Generar nuevo ID si no existe
        }

        try {
            // Encriptar la contraseña antes de guardarla
            String encryptedPassword = encryptPassword(contraseña, userId);

            // Crear el objeto UserPass (Password) con el orden correcto
            UserPass passwordObject = new UserPass(idTarjeta, userId, apuntes, encryptedPassword, sitio, usuario);  // Orden correcto

            // Consultar si ya existe un registro con este idTarjeta
            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("pass");
            String finalIdTarjeta = idTarjeta;
            databaseReference.orderByChild("idTarjeta").equalTo(idTarjeta).addListenerForSingleValueEvent( new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        // Si existe, actualizamos los datos
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            snapshot.getRef().child("nombrePagina").setValue(sitio);
                            snapshot.getRef().child("nombreUsuario").setValue(usuario);
                            snapshot.getRef().child("pass").setValue(encryptedPassword);
                            snapshot.getRef().child("apuntes").setValue(apuntes);
                        }

                        Toast.makeText(AddEditPasswordActivity.this, "Contraseña actualizada correctamente", Toast.LENGTH_SHORT).show();
                    } else {
                        // Si no existe, creamos uno nuevo usando idTarjeta como clave
                        databaseReference.child(finalIdTarjeta).setValue(passwordObject)
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(AddEditPasswordActivity.this, "Contraseña guardada correctamente", Toast.LENGTH_SHORT).show();
                                        finish();
                                    } else {
                                        Toast.makeText(AddEditPasswordActivity.this, "Error al guardar los datos", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Toast.makeText(AddEditPasswordActivity.this, "Error al acceder a la base de datos", Toast.LENGTH_SHORT).show();
                }
            });

        } catch (Exception e) {
            Toast.makeText(this, "Error al encriptar la contraseña", Toast.LENGTH_SHORT).show();
            Log.e("EncryptionError", e.getMessage());
        }
    }

    // Método para cifrar la contraseña con AES
    private String encryptPassword(String password, String userId) {
        try {
            // Usar el UID del usuario para generar la clave
            String key = generateKey(userId);  // Generar una clave a partir del userId

            // Cifrar la contraseña con AES
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);

            // Cifrar la contraseña
            byte[] encryptedBytes = cipher.doFinal(password.getBytes());

            // Convertir el array de bytes a un string codificado en Base64 para poder almacenarlo
            return Base64.encodeToString(encryptedBytes, Base64.DEFAULT);

        } catch (Exception e) {
            e.printStackTrace();
            return null;  // Si ocurre un error, devuelve null
        }
    }

    // Método para desencriptar la contraseña
    private String decryptPassword(String encryptedPassword) {
        try {
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            String key = generateKey(userId);

            // Desencriptar la contraseña con AES
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);

            byte[] decodedBytes = Base64.decode(encryptedPassword, Base64.DEFAULT);
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);

            return new String(decryptedBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return null;  // Si ocurre un error, devuelve null
        }
    }

    // Método para generar una clave a partir del userId
    private String generateKey(String userId) {
        if (userId.length() < 16) {
            while (userId.length() < 16) {
                userId += "0";  // Añadimos ceros hasta que sea de 16 caracteres
            }
        } else {
            // Si el userId es más largo, recortamos a los primeros 16 caracteres
            userId = userId.substring(0, 16);
        }

        return userId;  // Regresamos la clave de 16 caracteres
    }
}
