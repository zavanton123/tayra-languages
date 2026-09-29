# The sherpa-onnx C API with onnxruntime built in, as the project publishes it for iOS.
# Pulled by the Podfile so the ~70 MB framework never enters the repository.
Pod::Spec.new do |s|
  s.name         = 'SherpaOnnxC'
  s.version      = '1.13.8'
  s.summary      = 'Offline speech synthesis (Piper, Kokoro) from sherpa-onnx.'
  s.homepage     = 'https://github.com/k2-fsa/sherpa-onnx'
  s.license      = { :type => 'Apache-2.0' }
  s.author       = 'k2-fsa'
  s.platform     = :ios, '16.0'
  s.source       = { :http => "https://github.com/k2-fsa/sherpa-onnx/releases/download/xcframework/sherpa-onnx-v#{s.version}-ios-shared-onnxruntime-static.xcframework.zip" }
  s.vendored_frameworks = 'SherpaOnnxC.xcframework'
end
