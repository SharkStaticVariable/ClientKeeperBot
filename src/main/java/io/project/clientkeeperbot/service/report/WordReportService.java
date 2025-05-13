package io.project.clientkeeperbot.service.report;

import io.project.clientkeeperbot.entity.AdminResponse;
import io.project.clientkeeperbot.entity.Request;
import io.project.clientkeeperbot.entity.RequestStatus;
import io.project.clientkeeperbot.entity.WordReport;
import io.project.clientkeeperbot.repository.WordReportRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class WordReportService {

    private final WordReportRepository wordReportRepository;

    public WordReport generateAndSaveReport(Request request, AdminResponse response, Long adminId) throws IOException {
        File wordFile = generateWordFile(request, response);

        WordReport report = new WordReport();
        report.setRequest(request);
        report.setResponse(response);
        report.setFileName(wordFile.getName());
        report.setFilePath(wordFile.getAbsolutePath());
        report.setGeneratedAt(LocalDateTime.now());
        report.setAdminId(adminId);

        return wordReportRepository.save(report);
    }

//    private File generateWordFile(Request request, AdminResponse response) throws IOException {
//        // Используем Apache POI для создания документа
//        XWPFDocument doc = new XWPFDocument();
//
//        XWPFParagraph title = doc.createParagraph();
//        title.setAlignment(ParagraphAlignment.CENTER);
//        XWPFRun run = title.createRun();
//        run.setText("Сводка по заявке #" + request.getId());
//        run.setBold(true);
//        run.setFontSize(16);
//
//        addParagraph(doc, "Тип проекта: ", request.getType());
//        addParagraph(doc, "Описание: ", request.getDescription());
//        addParagraph(doc, "Сроки: ", request.getDeadline());
//        addParagraph(doc, "Бюджет: ", request.getBudget().toString());
//        addParagraph(doc, "Контакты: ", request.getContact());
//
//        if (response != null) {
//            addParagraph(doc, "Сейчас находится в статусе: ", response.getStatus().getDisplayName());
//            addParagraph(doc, "Комментарий менеджера: ", response.getResponseText());
//            addParagraph(doc, "Дата модерации: ", response.getResponseDate().toString());
//        }
//
//        File file = File.createTempFile("request_report_" + request.getId(), ".docx");
//        try (FileOutputStream out = new FileOutputStream(file)) {
//            doc.write(out);
//        }
//
//        return file;
//    }

    private File generateWordFile(Request request, AdminResponse response) throws IOException {
        XWPFDocument doc = new XWPFDocument();

        // Заголовок
        XWPFParagraph title = doc.createParagraph();
        title.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun run = title.createRun();
        run.setText("Сводка по заявке №" + request.getId());
        run.setBold(true);
        run.setFontSize(16);

        addSectionTitle(doc, "Информация о проекте");
        addParagraph(doc, "Тип проекта: ", request.getType());
        addParagraph(doc, "Описание: ",
                "Проект направлен на реализацию следующих задач: " + request.getDescription() +
                        ". Он должен быть завершён в срок " + request.getDeadline() + ", в рамках бюджета " +
                        request.getBudget() + " руб. Основное контактное лицо: " + request.getContact() + ".");

        if (response != null) {
            addSectionTitle(doc, "Модерация заявки");

            String statusText = response.getStatus() == RequestStatus.ACCEPTED
                    ? "Принята"
                    : "Отклонена";

            addParagraph(doc, "Текущий статус заявки: ", statusText);

            if (response.getResponseText() != null && !response.getResponseText().isBlank()) {
                addParagraph(doc, "Комментарий администратора: ", response.getResponseText());
            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy 'в' HH:mm", new Locale("ru"));
            addParagraph(doc, "Дата модерации: ", response.getResponseDate().format(formatter));
        }

        addSectionTitle(doc, "Выводы");
        addParagraph(doc, "", "Данная заявка была успешно рассмотрена и модерирована. " +
                "Клиент получил соответствующее уведомление, и дальнейшие действия могут быть предприняты в зависимости от её статуса.");

        // Сохраняем файл
        File file = File.createTempFile("request_report_" + request.getId(), ".docx");
        try (FileOutputStream out = new FileOutputStream(file)) {
            doc.write(out);
        }

        return file;
    }

    private void addSectionTitle(XWPFDocument doc, String titleText) {
        XWPFParagraph paragraph = doc.createParagraph();
        paragraph.setSpacingBefore(200);
        paragraph.setSpacingAfter(100);
        XWPFRun run = paragraph.createRun();
        run.setText(titleText);
        run.setBold(true);
        run.setFontSize(14);
        run.setColor("2E74B5");
    }
//    private void addParagraph(XWPFDocument doc, String title, String value) {
//        XWPFParagraph para = doc.createParagraph();
//        XWPFRun run = para.createRun();
//        run.setText(title + value);
//    }
    private void addParagraph(XWPFDocument doc, String label, String value) {
        XWPFParagraph paragraph = doc.createParagraph();
        paragraph.setSpacingBefore(100);
        XWPFRun run = paragraph.createRun();
        run.setBold(true);
        run.setText(label);
        run = paragraph.createRun();
        run.setText(value);
    }
}