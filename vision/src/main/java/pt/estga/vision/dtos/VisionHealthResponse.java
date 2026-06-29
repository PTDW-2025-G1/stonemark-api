package pt.estga.vision.dtos;

public record VisionHealthResponse(String status, String message) {

	public static VisionHealthResponse up() {
		return new VisionHealthResponse("UP", "Vision service is reachable");
	}

	public static VisionHealthResponse down(String detail) {
		return new VisionHealthResponse("DOWN", detail);
	}
}
