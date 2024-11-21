package activities;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.example.appmanagerpassword.R;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

import models.UserPass;

public class PasswordAdapter extends RecyclerView.Adapter<PasswordAdapter.PasswordViewHolder> {

    private List<UserPass> passwordList;
    private Context context;

    // Constructor
    public PasswordAdapter(List<UserPass> passwordList, Context context) {
        this.passwordList = passwordList;
        this.context = context;
    }

    // Crear la vista de cada item
    @Override
    public PasswordViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_password, parent, false);
        return new PasswordViewHolder(view);
    }

    // Enlazar los datos con la vista
    @Override
    public void onBindViewHolder(PasswordViewHolder holder, int position) {
        UserPass userPass = passwordList.get(position);

        holder.siteNameTextView.setText(userPass.getNombrePagina());
        holder.usernameTextView.setText(userPass.getNombreUsuario());
        holder.passwordTextView.setText("**********"); // Mostramos la contraseña como asteriscos
        holder.notesTextView.setText(userPass.getApuntes());

        // Configurar el botón de edición
        holder.editButton.setOnClickListener(v -> {
            editPassword(userPass);
        });

        // Configurar el botón de eliminar
        holder.deleteButton.setOnClickListener(v -> {
            deletePassword(userPass.getIdTarjeta(), position);
        });
    }

    // Número de items en la lista
    @Override
    public int getItemCount() {
        return passwordList.size();
    }

    // ViewHolder para cada item de la lista
    public static class PasswordViewHolder extends RecyclerView.ViewHolder {

        TextView siteNameTextView, usernameTextView, passwordTextView, notesTextView;
        Button deleteButton, editButton;

        public PasswordViewHolder(View itemView) {
            super(itemView);
            siteNameTextView = itemView.findViewById(R.id.siteNameTextView);
            usernameTextView = itemView.findViewById(R.id.usernameTextView);
            passwordTextView = itemView.findViewById(R.id.passwordTextView);
            notesTextView = itemView.findViewById(R.id.notesTextView);
            deleteButton = itemView.findViewById(R.id.deleteButton);
            editButton = itemView.findViewById(R.id.editButton); // Inicializamos el botón editar
        }
    }

    // Método para editar la contraseña
    private void editPassword(UserPass userPass) {
        // Descifrar la contraseña
        String decryptedPassword = decryptPassword(userPass.getPass());

        // Crear intent para ir a AddEditPasswordActivity
        Intent intent = new Intent(context, AddEditPasswordActivity.class);

        // Pasar los datos necesarios al intent
        intent.putExtra("idTarjeta", userPass.getIdTarjeta());
        intent.putExtra("nombrePagina", userPass.getNombrePagina());
        intent.putExtra("nombreUsuario", userPass.getNombreUsuario());
        intent.putExtra("password", decryptedPassword);
        intent.putExtra("apuntes", userPass.getApuntes());

        // Verificación de los datos que se están enviando (depuración)
        Log.d("PasswordAdapter", "Enviando: " + userPass.getNombrePagina() + " " + userPass.getNombreUsuario());

        // Iniciar la actividad
        context.startActivity(intent);
    }

    // Método para descifrar la contraseña
    private String decryptPassword(String encryptedPassword) {
        // Aquí puedes implementar la lógica para descifrar la contraseña
        // Ejemplo: usando una clave de desencriptación
        return new String(android.util.Base64.decode(encryptedPassword, android.util.Base64.DEFAULT));
    }

    // Método para eliminar la contraseña
    private void deletePassword(String idTarjeta, int position) {
        // Verificamos que la lista no esté vacía y que la posición sea válida
        if (passwordList != null && !passwordList.isEmpty() && position >= 0 && position < passwordList.size()) {
            // Obtenemos la referencia a la base de datos de Firebase
            DatabaseReference database = FirebaseDatabase.getInstance().getReference("pass");

            // Eliminamos el nodo correspondiente a la tarjeta usando su idTarjeta
            database.child(idTarjeta).removeValue()
                    .addOnSuccessListener(aVoid -> {
                        // Si la eliminación fue exitosa en Firebase, eliminamos también de la lista local
                        if (position < passwordList.size()) {
                            passwordList.remove(position); // Eliminamos de la lista local
                            notifyItemRemoved(position);    // Notificamos al adaptador que se eliminó un item
                            Toast.makeText(context, "Contraseña eliminada correctamente", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Si hay un error al eliminar, mostramos un mensaje
                        Toast.makeText(context, "Error al eliminar la contraseña", Toast.LENGTH_SHORT).show();
                    });
        } else {
            // Si la lista está vacía o la posición es inválida, mostramos un mensaje
            Toast.makeText(context, "No hay contraseñas para eliminar o la posición es incorrecta", Toast.LENGTH_SHORT).show();
        }
    }
}
