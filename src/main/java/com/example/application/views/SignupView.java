package com.example.application.views;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.gridpro.GridPro;
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

    private final GridPro<Participant> grid = new GridPro<>();

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
        grid.setEditOnClick(true);

        grid.addEditColumn(Participant::name)
            .text((participant, newValue) -> updateParticipant(participant, newValue, participant.company(), participant.email(), participant.tshirtSize()))
            .setHeader("Name")
            .setSortable(true)
            .setAutoWidth(true);

        grid.addEditColumn(Participant::company)
            .text((participant, newValue) -> updateParticipant(participant, participant.name(), newValue, participant.email(), participant.tshirtSize()))
            .setHeader("Company")
            .setSortable(true)
            .setAutoWidth(true);

        grid.addEditColumn(Participant::email)
            .text((participant, newValue) -> updateParticipant(participant, participant.name(), participant.company(), newValue, participant.tshirtSize()))
            .setHeader("Email")
            .setSortable(true)
            .setAutoWidth(true);

        grid.addEditColumn(Participant::tshirtSize)
            .text((participant, newValue) -> updateParticipant(participant, participant.name(), participant.company(), participant.email(), newValue))
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

    private void updateParticipant(Participant oldParticipant, String name, String company, String email, String tshirtSize) {
        // Create a new Participant with updated fields
        Participant updated = new Participant(name, company, email, tshirtSize);

        // Replace the old participant in the list
        int index = participants.indexOf(oldParticipant);
        if (index >= 0) {
            participants.set(index, updated);
            grid.setItems(participants);
        }
    }
}