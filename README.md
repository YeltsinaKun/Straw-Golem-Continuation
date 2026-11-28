# Straw Golem Continuation

Có hôm ra thăm ruộng, bạn sẽ thấy cả quả dưa hấu đang lững thững đi về phía rương. Nhìn xuống dưới một chút sẽ thấy chú golem rơm đang đội nó trên đầu. Cậu chàng này biết gặt lúa, trồng lại cà rốt, hái quả mọng và mang nông sản về cho bạn. Chỉ có điều, nếu định cho nó làm hàng xóm với mấy con bò thì nhớ dựng hàng rào nhé.

Bản mod dành cho **Minecraft Java 26.1.2 trên Fabric**. Để chơi, cài Fabric Loader rồi bỏ Fabric API và file JAR của mod vào thư mục `mods/`.

Mod ID hiện là `straw-golem-continuation`. World dùng bản có ID `straw-golem-relaunched` cần được sao lưu trước khi thử bản này. Golem và vật phẩm đã lưu với ID cũ cần chuyển ID riêng để bản mới đọc được.

## Đón golem về ruộng

1. Đặt một **khối rơm** bên ruộng rồi đặt **bí ngô khắc** chạm vào khối rơm. Đặt trên đầu cho có dáng người rơm cũng được, gắn bên hông cũng được. Cả sáu mặt đều dùng được, miễn là bạn đặt bí ngô sau.
2. Để trống tay chính, giữ **Shift** rồi nhấp phải vào golem. Vẫn để tay trống và giữ Shift, nhấp phải vào chiếc rương bạn muốn nó mang đồ về. Dùng thùng gỗ cũng được. Nhớ chừa lối cho đôi chân ngắn của nó đi tới.
3. Thử cho nó một luống lúa mì, cà rốt, khoai tây hoặc củ dền đã chín. Khi tay chính trống, nó sẽ tự tìm cây để gặt. Hễ có nông sản trên tay sau một lần gặt, nó tìm chỗ cất ngay. Cất hết đồ trên tay rồi nó mới gặt tiếp.

## Gặt xong thì lấy gì trồng lại?

Cứ lấy một cây cà rốt cho đúng một chồng **4 củ** làm ví dụ. Chỉnh `vanilla_harvesting` trong config sẽ quyết định bạn nhận được mấy củ từ cây ấy.

- **Bật (`true`, mặc định).** Golem lấy 1 củ cắm lại xuống đất rồi mang **3 củ** về rương. Khoai tây cũng vậy. Với lúa mì và củ dền, nó lấy một hạt từ vụ vừa gặt để gieo. Nếu lần đó chẳng được hạt nào, nó lấy hạt giống đã để dành. Hết cả giống thì ô đất ấy sẽ để trống.
- **Tắt (`false`).** Golem mang về **cả 4 củ**, trên luống vẫn có cây con. Mỗi lần gặt kiểu này, nó chỉ lấy một chồng đồ. Chẳng hạn, nếu cây lúa cho cả lúa mì lẫn hạt giống và golem lấy chồng lúa mì, phần hạt sẽ không rơi trên ruộng hay được cất làm giống.

Bí ngô và dưa hấu thì đơn giản hơn. Ở cả hai chế độ, golem hái quả đã mọc từ thân rồi để thân cây ở đó cho ra lứa sau. Riêng dưa hấu, bạn sẽ nhận được nguyên quả.

## Nó mang đồ về đâu?

Golem nhớ chiếc rương bạn chỉ cho nó. Nếu rương đầy hoặc bị phá, nó thử lại chỗ vừa giao đồ thành công, sau đó mới tìm quanh mình. Rương chỉ còn chỗ cho 2 củ thì nó cất 2 củ, số còn lại vẫn cầm theo. Bạn đổi rương giữa đường cũng được, nó sẽ dùng rương mới từ chuyến sau.

Có lúc bạn thấy nó ôm đồ mãi mà ruộng bên cạnh đã chín hết. Thử dọn vài ô trong rương hoặc đặt thêm thùng gỗ ở chỗ nó đi tới được. Chừng nào chưa tìm được chỗ cất, cậu ta vẫn còn bận ôm chỗ nông sản ấy mà ngẫm nghĩ về cuộc đời.

### Còn hạt dư thì sao?

Khi bật `vanilla_harvesting`, golem để dành tối đa 4 hạt giống mỗi loại. Giả sử nó đang có 3 hạt lúa mì và gặt thêm được 4 hạt nữa. Nó gieo lại 1 hạt, để dành thêm 1 hạt cho tròn 4, vậy là dư 2. Nó mang lúa mì về trước, cất xong mới quay sang lo cho 2 hạt đó.

Bình thường hạt dư cũng về rương. Muốn tận dụng chúng làm bột xương, bạn có thể bật `auto_compost_seeds` rồi đặt một thùng ủ gần ruộng. Golem sẽ đem hạt đi ủ nếu tới được thùng. Nếu chẳng tìm được thùng nào phù hợp, nó lại mang hạt về cất.

## Chăm chú golem của bạn

Golem rơm mặc định có **6 tim**. Nếu thấy rơm trên người nó đã sờn sau một trận rượt đuổi ngoài ruộng, cầm **bó rơm** ở tay chính rồi nhấp phải vào nó. Mỗi bó hồi được tối đa **1,5 tim**. Còn muốn chú golem nhà mình khỏe hơn từ đầu, bạn có thể chỉnh máu tối đa trong config.

Tuổi thọ mặc định tắt. Nếu bật `lifespan_enabled`, rơm sẽ mòn dần, golem mất máu theo tuổi rồi cuối cùng cũng già đi mà chết. Ở chỗ khô, với cấu hình mặc định và ruộng được tải liên tục, một chú khỏe mạnh sống khoảng 80 phút nếu bạn không đắp thêm rơm cho nó. Bị đánh hay bị gặm thì nó có thể chết sớm hơn.

Khi bật tuổi thọ, đứng trong nước hoặc đội mưa lâu sẽ làm rơm tích ẩm. Một khi đủ ướt, golem già nhanh gấp đôi. Dẫn nó vào chỗ khô thì rơm sẽ ráo dần. Nếu hay gặp mưa giữa buổi gặt, cho nó đội một chiếc **nón rơm**. Nón che được mưa, còn lội xuống nước thì nó vẫn ướt như thường.

Và nhớ để mắt tới mấy con bò, cừu trong trại. Với bạn thì đây là người làm vườn, với chúng thì đây là một bữa buffet biết đi. Nếu bị gặm đến hoảng, golem có thể quăng cả chồng đồ đang cầm rồi bỏ chạy.

## Chỉnh sửa theo ý bạn

Chạy game lần đầu để mod tạo file `config/strawgolem.json`. Mở file, chỉnh mục bạn muốn rồi khởi động lại game hoặc máy chủ. Ba mục đầu trong bảng nằm trong nhóm `attributes`.

| Mục | Mặc định | Dùng để chỉnh gì? |
| --- | --- | --- |
| `attributes.max_health` | `12` | Máu tối đa, tính theo điểm máu. Cứ 2 điểm là 1 tim. |
| `attributes.movement_speed` | `0.25` | Tốc độ di chuyển của golem. |
| `attributes.wander_max_distance` | `24` | Khoảng cách golem đi lang thang lúc rảnh. |
| `vanilla_harvesting` | `true` | Bật để dùng hạt hoặc củ khi trồng lại. Tắt để cây mọc lại miễn phí và chỉ lấy một chồng đồ mỗi lần gặt. |
| `auto_compost_seeds` | `false` | Bật để golem mang hạt dư đi ủ và thu bột xương. |
| `lifespan_enabled` | `false` | Bật để golem già đi theo thời gian. |
| `lifespan_seconds` | `4800` | Tuổi thọ cơ bản ở chỗ khô, tính bằng giây. |
| `harvest_horizontal_range` | `24` | Bán kính tìm cây chín quanh golem, tính bằng block. |
| `harvest_scan_vertical_range` | `12` | Golem tìm cây bao nhiêu block lên trên và xuống dưới. |
| `harvest_target_vertical_range` | `3` | Cây được chọn để gặt có thể cao hoặc thấp hơn golem bao nhiêu block. |
| `deposit_horizontal_range`, `deposit_vertical_range` | `32`, `8` | Khi tự tìm chỗ cất đồ, golem tìm tối đa 32 block theo mỗi trục ngang và 8 block lên hoặc xuống. |

## Ghi công & Giấy phép

Dự án này là bản port không chính thức của **Straw Golem Rebaled**, được phát triển ban đầu bởi **CommodoreThrawn**.

- Mod gốc: https://www.curseforge.com/minecraft/mc-mods/straw-golem-reborn
- Mã nguồn gốc: https://github.com/godemperoroftheworld/strawgolem
- Mã nguồn và tài nguyên gốc thuộc bản quyền của các tác giả tương ứng.
- Chuyển phiên bản, cập nhật tương thích và phát triển bổ sung bởi **Xuân Bách Trần**.

Dự án này hoạt động độc lập, không đại diện cho tác giả gốc và chưa được tác giả gốc xác nhận là bản port chính thức.

Dự án được phân phối theo **Giấy phép Công cộng GNU Affero phiên bản 3 (AGPLv3)**. Xem [LICENSE](LICENSE) để biết đầy đủ điều khoản.

