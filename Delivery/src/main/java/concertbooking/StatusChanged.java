
package concertbooking;

public class StatusChanged extends AbstractEvent {

    private Long bookingId;
    private Long ccId;
    private String ccName;
    private String bookingStatus;
    private String paymentStatus;

    public Long getId() {
        return bookingId;
    }

    public void setId(Long bookingId) {
        this.bookingId = bookingId;
    }
    public Long getCcId() {
        return ccId;
    }

    public void setCcId(Long ccId) {
        this.ccId = ccId;
    }
    public String getCcName() {
        return ccName;
    }

    public void setCcName(String ccName) {
        this.ccName = ccName;
    }
    public String getBookingStatus() {
        return bookingStatus;
    }

    public void setBookingStatus(String bookingStatus) {
        this.bookingStatus = bookingStatus;
    }
    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }
}

