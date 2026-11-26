# Straw Golem Continuation

Straw Golem Continuation tiếp nối bản Straw Golem của **t2pellet** cho **Minecraft Java 26.2 trên Fabric**. Đặt một kiện rơm bên ruộng, gắn bí ngô khắc lên đó rồi chỉ cho golem chiếc rương của bạn. Nó sẽ tự tìm cây chín, thu hoạch, trồng lại và mang nông sản về. Nếu thấy nó ôm cả quả dưa hấu trên đầu, đó là chuyến giao hàng tiếp theo.

**Bản đang phát triển:** `0.1.0-beta.1+26.2` · **Fabric Loader:** `0.20.1` · **Fabric API dùng để build:** `0.161.0+26.2` · **Java:** `25`

## Bắt đầu trong Survival

1. **Tạo golem.** Đặt một kiện rơm, sau đó đặt bí ngô khắc chạm một trong sáu mặt của kiện. Khi golem xuất hiện, hai khối được tiêu thụ.
2. **Giao rương.** Để trống tay chính, giữ **Shift** rồi nhấp phải vào golem. Vẫn để tay chính trống, giữ **Shift** và nhấp phải vào rương. Chừa lối đi để golem có thể tới gần.
3. **Để cây chín.** Thử với một luống lúa mì, cà rốt, khoai tây hoặc củ dền. Golem bắt đầu tìm cây khi tay trống. Nếu rương chỉ nhận được một phần nông sản, nó giữ phần còn lại trên tay và tiếp tục tìm chỗ cất.

## Trên ruộng

- **Gặt và trồng lại.** Theo mặc định, golem dùng hạt lúa mì, hạt củ dền, cà rốt hoặc khoai tây để trồng lại cây tương ứng. Nếu thiếu vật phẩm trồng, ô đất sẽ để trống. Golem còn hái quả mọng ngọt và thu hoạch bí ngô, dưa hấu mọc từ thân cây. Dưa hấu được mang về nguyên khối.
- **Nhặt đồ.** Golem mang mỗi lần một chồng vật phẩm. Nó có thể nhặt nông sản rơi và giữ riêng tối đa bốn hạt mỗi loại cho lúa mì, củ dền, dưa hấu và bí ngô.

### Golem chọn rương nào?

Đầu mỗi chuyến cất đồ, golem thử lần lượt:

1. Rương bạn chỉ định.
2. Rương mà golem vừa giao đồ thành công.
3. Các rương hoặc kho khác quanh golem.

Nếu rương đầy, bị phá hoặc không tới được, golem thử lựa chọn tiếp theo. Khi hết chỗ cất, nó giữ đồ trên tay và chờ. Nếu bạn đổi rương chỉ định lúc golem đang đi giao, nó dùng thứ tự mới từ chuyến kế tiếp.

### Hạt dư đi đâu?

Khi bật `vanilla_harvesting`, golem **cất nông sản trước, xử lý hạt dư sau**. Ví dụ, nếu đang giữ 3 hạt lúa mì và gặt thêm 4 hạt, nó dùng 1 hạt để trồng lại, giữ 4 hạt làm giống và còn 2 hạt dư. Sau khi cất lúa mì, nó mới cầm 2 hạt đó lên tay.

- **Mặc định**, golem mang hạt dư về rương.
- **Khi bật `auto_compost_seeds`**, golem ưu tiên thùng ủ mà nó có thể tới được. Nếu thiếu thùng phù hợp, nó mang hạt về rương. Nếu đang mang 5 hạt và thùng bị phá sau khi đã dùng 2, nó đem đúng 3 hạt còn lại đi cất.
- **Khi hết chỗ nhận**, golem giữ hạt trên tay và tạm ngừng làm nông. Bạn có thể dọn chỗ trong rương hoặc đặt thêm rương ở nơi nó tới được để nó làm tiếp. Trong lúc chờ, nó vẫn né động vật và có thể chạy hoảng.

Nếu bạn lưu và thoát giữa chuyến, golem giữ đồ trên tay, hạt dự trữ và hạt dư. Khi mở lại thế giới, nó có thể tiếp tục chuyến đang làm.

## Chăm sóc golem

| Tình huống | Điều xảy ra |
| --- | --- |
| Golem bị thương | Golem mặc định có 6 tim. Cầm lúa mì (`minecraft:wheat`) ở tay chính rồi nhấp phải để hồi tối đa 1,5 tim. |
| Trời mưa hoặc rét | Golem run khi bị mưa tạt hoặc ở nơi đủ lạnh để có tuyết. Mái che và mũ rơm giúp nó tránh mưa. Khi trời rét, nó vẫn run dù đội mũ. |
| Làm mũ rơm | Trên bàn chế tạo, đặt một lúa mì ở ô giữa hàng trên và ba lúa mì ở hàng kế tiếp. Cầm mũ ở tay chính rồi nhấp phải vào golem. |
| Có động vật ở gần | Bò, cừu, bò nấm, ngựa, lừa, la, llama và llama thương nhân có thể đến gặm golem trong phạm vi 6 block. Golem có thể né và giữ đồ nếu thấy con vật ở phía trước, con vật có đường tới nó và nó tìm được đường thoát. |
| Golem chạy hoảng | Một cú gặm trúng gây 0,5 điểm máu và có thể khiến golem chạy hoảng. Khi bắt đầu chạy hoảng, nó chỉ quăng chồng đồ đang cầm, kể cả khi đang cháy hoặc ở trong dung nham. Lửa, lửa trại và dung nham gây gấp đôi sát thương. |
| Golem chết | Đồ còn trên tay, hạt giống dự trữ, hạt dư chờ giao và các chồng đồ đang chờ thả đều rơi tại chỗ. |

**Ngoại hình theo máu.** Khi còn từ 6 HP trở xuống, golem trông cũ đi. Từ 2 HP trở xuống, nó dùng texture rất cũ. Hai ngưỡng này luôn áp dụng, kể cả khi tắt tuổi thọ. Cho ăn lúa mì có thể giúp golem trông khỏe lại.

**Tuổi thọ là tùy chọn, mặc định tắt.** Khi bật, golem sống khoảng 80 phút ở nơi khô nếu game chạy ở 20 tick/giây và golem được tải liên tục. Rơm mòn dần làm giảm máu thật. Với 12 HP và tuổi thọ 80 phút, golem mất 1 HP mỗi 6 phút 40 giây ở nơi khô. Bị đánh làm mất thêm máu ngay và có thể khiến nó chết sớm. Một lúa mì hồi tối đa 3 HP và bù 20 phút tuổi thọ với cấu hình này. Khi đắp lúa đến đầy máu, tuổi thọ được khôi phục đầy. Mưa trực tiếp khi đầu trần hoặc ngâm nước làm rơm tích ẩm. Khi đủ ẩm, golem già nhanh gấp đôi. Đưa nó về chỗ khô để độ ẩm giảm dần. Golem giữ tuổi, độ ẩm và mũ khi bạn mở lại thế giới.

## Cài đặt và cấu hình

Cài Fabric Loader cho Minecraft Java 26.2. Đặt Fabric API dành cho 26.2 và JAR chạy của Straw Golem Continuation vào thư mục `mods/`. Nếu chơi trên máy chủ riêng, cài mod và Fabric API cho cả máy chủ lẫn máy người chơi. Bạn có thể tự tạo JAR từ mã nguồn theo hướng dẫn bên dưới.

Sau lần chạy đầu, chỉnh `config/strawgolem.json` trong thư mục game hoặc máy chủ rồi khởi động lại. Các tùy chọn thường dùng gồm:

| Tùy chọn | Mặc định | Tác dụng |
| --- | --- | --- |
| `vanilla_harvesting` | `true` | Dùng vật phẩm để trồng lại. Đặt `false` để gặt theo chế độ đơn giản, chọn một chồng sản phẩm và đặt lại cây mà không dùng hạt. Dưa hấu vẫn cho nguyên khối. |
| `auto_compost_seeds` | `false` | Cho phép ủ hạt và thu bột xương. |
| `lifespan_enabled`, `lifespan_seconds` | `false`, `4800` | Bật tuổi thọ và đặt thời gian sống cơ bản theo giây. |
| `harvest_horizontal_range` | `24` | Bán kính ngang để chọn cây. |
| `harvest_scan_vertical_range`, `harvest_target_vertical_range` | `12`, `3` | Quét lên và xuống tối đa 12 block, chọn cây chênh cao tối đa 3 block. |
| `deposit_horizontal_range`, `deposit_vertical_range` | `32`, `8` | Giới hạn tìm kho tự do trên mỗi trục ngang và theo chiều cao. |

## Build từ mã nguồn

Cài **JDK 25**, mở terminal ở thư mục chứa `gradlew` và chạy lệnh tương ứng:

| Mục đích | Windows PowerShell | Linux / macOS |
| --- | --- | --- |
| Build JAR | `.\gradlew.bat build` | `sh ./gradlew build` |
| Chạy client phát triển | `.\gradlew.bat runClient` | `sh ./gradlew runClient` |

Sau khi build, dùng `build/libs/straw-golem-relaunched-0.1.0-beta.1+26.2.jar` để chơi. File có đuôi `-sources.jar` chứa mã nguồn.

## Báo lỗi

Hãy cho biết phiên bản Minecraft, Fabric Loader, Fabric API và mod. Với lỗi làm nông, mô tả loại cây, đồ trên tay golem, vị trí rương và các bước để thấy lại lỗi. Với lỗi hoạt ảnh, ảnh chụp hoặc video ngắn sẽ giúp tái hiện. [GitHub Issues](https://github.com/YeltsinaKun/straw-golem-relaunched/issues)

## Giấy phép và ghi công

Straw Golem Relaunched là bản chuyển thể cộng đồng dành cho Fabric 26.2, phát triển bởi **Xubache1407**. Dự án dùng giấy phép **AGPL-3.0-only**. [Giấy phép của repository](LICENSE)

Cảm ơn **t2pellet** vì bản Straw Golem mà dự án này tiếp nối, **Fr3nderman** vì model gốc và **CommodoreThrawn** vì thiết kế ban đầu. Xem [NOTICE](NOTICE) để biết nguồn các tài nguyên đi kèm. [Straw Golem 2.2.0](https://modrinth.com/mod/straw-golem/version/2.2.0)
