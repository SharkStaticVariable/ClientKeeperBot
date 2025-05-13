package io.project.clientkeeperbot.service.report;

import io.project.clientkeeperbot.entity.AdminResponse;
import io.project.clientkeeperbot.entity.Request;
import io.project.clientkeeperbot.entity.RequestCountChart;
import io.project.clientkeeperbot.repository.AdminResponseRepository;
import io.project.clientkeeperbot.repository.RequestCountChartRepository;
import io.project.clientkeeperbot.repository.RequetRepository;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.ItemLabelAnchor;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.TreeMap;
import java.util.TreeSet;


@Service
public class ChartReportService {

    @Autowired
    private RequetRepository requestRepository;
    @Autowired
    private RequestCountChartRepository chartRepository;
    @Autowired
    private AdminResponseRepository adminResponseRepository;

    public Map<LocalDate, Map<String, Long>> getAdminActivityPerDay(LocalDate startDate, LocalDate endDate) {
        List<AdminResponse> responses = adminResponseRepository.findAllByResponseDateBetween(
                startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay()
        );

        return responses.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getResponseDate().toLocalDate(),
                        TreeMap::new,
                        Collectors.groupingBy(
                                r -> r.getAdminId().toString(), // или getFirstName() / getId()
                                Collectors.counting()
                        )
                ));
    }

    private File generateAdminActivityChartFile(LocalDate from, LocalDate to) throws IOException {
        Map<LocalDate, Map<String, Long>> activity = getAdminActivityPerDay(from, to);

        // Собираем список всех админов, чтобы гарантировать одинаковый порядок
        Set<String> allAdmins = activity.values().stream()
                .flatMap(map -> map.keySet().stream())
                .collect(Collectors.toCollection(TreeSet::new)); // TreeSet для сортировки

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        for (Map.Entry<LocalDate, Map<String, Long>> entry : activity.entrySet()) {
            LocalDate date = entry.getKey();
            Map<String, Long> countsByAdmin = entry.getValue();
            for (String admin : allAdmins) {
                long count = countsByAdmin.getOrDefault(admin, 0L);
                dataset.addValue(count, admin, date.toString());
            }
        }

        JFreeChart chart = ChartFactory.createBarChart(
                "Активность менеджеров по дням",
                "Дата",
                "Количество обработанных заявок",
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false
        );

        chart.setBackgroundPaint(Color.WHITE);

        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(new Color(245, 245, 245));
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.GRAY);

        // Шрифты
        chart.getTitle().setFont(new Font("SansSerif", Font.BOLD, 18));
        plot.getDomainAxis().setTickLabelFont(new Font("SansSerif", Font.PLAIN, 12));
        plot.getDomainAxis().setLabelFont(new Font("SansSerif", Font.BOLD, 14));
        plot.getRangeAxis().setTickLabelFont(new Font("SansSerif", Font.PLAIN, 12));
        plot.getRangeAxis().setLabelFont(new Font("SansSerif", Font.BOLD, 14));

        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

        CategoryAxis domainAxis = plot.getDomainAxis();
        domainAxis.setCategoryLabelPositions(CategoryLabelPositions.createUpRotationLabelPositions(Math.PI / 4));

        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setDefaultItemLabelsVisible(true);
        renderer.setDefaultItemLabelFont(new Font("SansSerif", Font.BOLD, 12));
        renderer.setItemMargin(0.1);
        renderer.setMaximumBarWidth(0.7);
        renderer.setSeriesPaint(0, Color.BLUE); renderer.setSeriesPaint(1, Color.ORANGE);

        File imageFile = File.createTempFile("admin_activity_chart_", ".png");
        ChartUtils.saveChartAsPNG(imageFile, chart, 1000, 600);
        return imageFile;
    }

    public RequestCountChart generateAndSaveChart(LocalDate from, LocalDate to, Long adminId) throws IOException {

        File file = generateRequestCountChart(from, to);

        RequestCountChart chart = new RequestCountChart();
        chart.setFromDate(from);
        chart.setToDate(to);
        chart.setGeneratedByAdminId(adminId);
        chart.setGeneratedAt(LocalDateTime.now());
        chart.setFilePath(file.getAbsolutePath());

        return chartRepository.save(chart);
    }

    public RequestCountChart generateAndSaveChartAdmin(LocalDate from, LocalDate to, Long adminId) throws IOException {

        File file = generateAdminActivityChartFile(from, to);

        RequestCountChart chart = new RequestCountChart();
        chart.setFromDate(from);
        chart.setToDate(to);
        chart.setGeneratedByAdminId(adminId);
        chart.setGeneratedAt(LocalDateTime.now());
        chart.setFilePath(file.getAbsolutePath());

        return chartRepository.save(chart);
    }
    private File generateRequestCountChart(LocalDate from, LocalDate to) throws IOException {

        Map<LocalDate, Long> counts = getRequestCountsByDate(from, to);

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (Map.Entry<LocalDate, Long> entry : counts.entrySet()) {
            String dateStr = entry.getKey().toString(); // YYYY-MM-DD
            dataset.addValue(entry.getValue(), "Заявки", dateStr);
        }

        JFreeChart chart = ChartFactory.createBarChart(
                "Заявки по дням",     // Заголовок
                "Дата",               // Ось X
                "Количество заявок",  // Ось Y
                dataset
        );

        chart.setBackgroundPaint(Color.WHITE);

        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(new Color(245, 245, 245)); // светло-серый фон
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.GRAY);

        // Настройка шрифта заголовка
        chart.getTitle().setFont(new Font("SansSerif", Font.BOLD, 18));

        // Подписи осей
        plot.getDomainAxis().setTickLabelFont(new Font("SansSerif", Font.PLAIN, 12));
        plot.getDomainAxis().setLabelFont(new Font("SansSerif", Font.BOLD, 14));
        plot.getRangeAxis().setTickLabelFont(new Font("SansSerif", Font.PLAIN, 12));
        plot.getRangeAxis().setLabelFont(new Font("SansSerif", Font.BOLD, 14));

        // Целочисленная шкала Y
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

        CategoryAxis domainAxis = plot.getDomainAxis();
        domainAxis.setCategoryLabelPositions(CategoryLabelPositions.createUpRotationLabelPositions(Math.PI / 4)); // 45 градусов
        // Отображение значений над столбцами
        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setDefaultItemLabelsVisible(true);
        renderer.setDefaultItemLabelFont(new Font("SansSerif", Font.BOLD, 12));
        renderer.setSeriesPaint(0, new Color(79, 129, 189)); // Цвет столбцов (синий)

// 💡 Ограничиваем ширину столбцов (0.1 = 10% от ширины категории)
        renderer.setMaximumBarWidth(0.7);

        File imageFile = File.createTempFile("requests_bar_chart_", ".png");
        ChartUtils.saveChartAsPNG(imageFile, chart, 900, 600);
        return imageFile;
    }

    private Map<LocalDate, Long> getRequestCountsByDate(LocalDate from, LocalDate to) {
        List<Request> requests = requestRepository.findByCreatedAtBetween(
                from.atStartOfDay(),
                to.atTime(23, 59, 59)
        );

        System.out.println("Найдено заявок: " + requests.size());
        requests.forEach(r -> System.out.println(r.getCreatedAt()));

        return requests.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getCreatedAt().toLocalDate(),
                        TreeMap::new,
                        Collectors.counting()
                ));
    }
}