package com.example.application.views;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.InputStreamResource;
import org.springframework.util.MimeTypeUtils;

import java.util.ArrayList;
import java.util.List;

@Route("signup")
@PageTitle("Signup")
@Menu(title = "Signup", order = 2)
public class SignupView extends VerticalLayout {

    public record Participant(String name, String company, String email, String tshirtSize) {
    }

    public record SignUpSheet(List<Participant> participants) {
    }

    private final Grid<Participant> grid = new Grid<>();

    private List<Participant> participants = new ArrayList<>();

    public SignupView(ChatClient.Builder builder) {
        var client = builder.build();

        // Set up upload
        var buffer = new MemoryBuffer();
        var upload = new Upload(buffer);
        upload.setAcceptedFileTypes("image/*");
        upload.setMaxFileSize(10 * 1024 * 1024);

        upload.addSucceededListener(e -> {
            var signUpSheet = client.prompt()
                .user(userMessage -> userMessage
                    .text("""
                        Please read the attached event signup sheet image and extract all participants.
                        """)
                    .media(
                        MimeTypeUtils.parseMimeType(e.getMIMEType()),
                        new InputStreamResource(buffer.getInputStream())
                    )
                )
                .call()
                .entity(SignUpSheet.class);

            showParticipants(signUpSheet);
            upload.clearFileList();
        });

        Text instructions = new Text("Upload an image of the event signup sheet. The AI will extract participant data and display it here.");
        add(instructions, upload, createGridLayout());
    }

    private Div createGridLayout() {
        Div gridContainer = new Div();
        gridContainer.setWidthFull();

        grid.setSizeFull();
        grid.setItems(participants);
        grid.setAllRowsVisible(true);

        grid.addColumn(Participant::name)
                .setHeader("Name")
                .setSortable(true)
                .setAutoWidth(true);

        grid.addColumn(Participant::company)
                .setHeader("Company")
                .setSortable(true)
                .setAutoWidth(true);

        grid.addColumn(Participant::email)
                .setHeader("Email")
                .setSortable(true)
                .setAutoWidth(true);

        grid.addColumn(Participant::tshirtSize)
                .setHeader("T-Shirt Size")
                .setSortable(true)
                .setAutoWidth(true);

        gridContainer.add(grid);
        return gridContainer;
    }

    private void showParticipants(SignUpSheet signUpSheet) {
        if (signUpSheet != null && signUpSheet.participants() != null) {
            participants = new ArrayList<>(signUpSheet.participants());
            grid.setItems(participants);
        }
    }

}